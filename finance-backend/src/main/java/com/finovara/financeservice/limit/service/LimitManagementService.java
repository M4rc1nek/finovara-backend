package com.finovara.financeservice.limit.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitManagementService implements UserDataDeletable {
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final LimitExpensesValidator limitExpensesValidator;
    private final OutboxService outboxService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public Long createLimit(LimitDto limitDto, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(limitDto.authorizationCode()));

        limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(userId, limitDto);

        if (limitDto.category() == null) {
            limitRepository.findGeneralLimit(userId, limitDto.periodType())
                    .ifPresent(limit -> {
                        throw new EntityAlreadyExistsException("General limit already exists");
                    });
        } else {
            limitRepository.findCategoryLimit(userId, limitDto.periodType(), limitDto.category())
                    .ifPresent(limit -> {
                        throw new EntityAlreadyExistsException("Category limit already exists");
                    });
        }

        Limit limit = Limit.builder()
                .periodType(limitDto.periodType())
                .category(limitDto.category())
                .amount(limitDto.amount())
                .isActive(true)
                .userId(userId)
                .build();

        Limit savedLimit = limitRepository.save(limit);
        outboxService.save("Limit", savedLimit.getId().toString(), "activity.limit",
                new LimitActivityEvent(userId, LimitActivityType.ADDED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));

        return savedLimit.getId();
    }

    @Transactional
    public Long editLimit(LimitDto limitDto, Long limitId, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(limitDto.authorizationCode()));

        Limit limit = limitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));

        if (!limit.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Active Limit not found for this user");
        }

        limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(userId, limitDto);

        BigDecimal oldLimitAmount = limit.getAmount();

        limit.setPeriodType(limitDto.periodType());
        limit.setCategory(limitDto.category());
        limit.setAmount(limitDto.amount());

        limitRepository.save(limit);
        outboxService.save("Limit", limitId.toString(), "activity.limit",
                new LimitActivityEvent(userId, LimitActivityType.EDITED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), oldLimitAmount, LocalDateTime.now()));

        return limitId;
    }

    public List<LimitStatsDto> getLimitStats(Long userId) {
        List<Limit> limits = limitRepository.findAllByUserId(userId);
        LocalDate today = LocalDate.now();

        return limits.stream()
                .map(limit ->
                        limitCalculateService.calculateLimitStats(userId, limit.getId(), today))
                .toList();
    }

    @Transactional
    public void deleteLimit(Long userId, Long limitId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        Limit limit = limitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));

        outboxService.save("Limit", limitId.toString(), "activity.limit",
                new LimitActivityEvent(userId, LimitActivityType.DELETED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));
        limitRepository.delete(limit);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        limitRepository.deleteByUserId(userId);
        log.info("Deleted limit for userId={}", userId);
    }

}
