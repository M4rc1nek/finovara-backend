package com.finovara.financeservice.limit.service;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitManagementService implements UserDataDeletable {
    private final LimitRepository limitRepository;
    private final LimitManagerService limitManagerService;
    private final LimitCalculateService limitCalculateService;
    private final LimitExpensesValidator limitExpensesValidator;
    private final OutboxService outboxService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    private final WalletService walletService;

    @Transactional
    public Long createLimit(LimitDto limitDto, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(limitDto.authorizationCode()));

        walletService.lockWalletForUpdate(userId);
        limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(userId, limitDto);
        throwIfLimitAlreadyExists(userId, limitDto, null);

        Limit limit = Limit.builder()
                .periodType(limitDto.periodType())
                .category(limitDto.category())
                .amount(limitDto.amount())
                .isActive(true)
                .userId(userId)
                .build();

        Limit savedLimit = limitRepository.save(limit);

        outboxService.save("Limit", savedLimit.getId().toString(), "limit.changed",
                new LimitActivityEvent(userId, LimitActivityType.ADDED_LIMIT, savedLimit.getPeriodType().name(), savedLimit.getAmount(), null, LocalDateTime.now()));

        return savedLimit.getId();
    }

    @Transactional
    public Long editLimit(LimitDto limitDto, Long limitId, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(limitDto.authorizationCode()));

        walletService.lockWalletForUpdate(userId);
        Limit limit = limitManagerService.getLimitByUserIdOrThrow(userId, limitId);

        limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(userId, limitDto);
        throwIfLimitAlreadyExists(userId, limitDto, limitId);

        BigDecimal oldLimitAmount = limit.getAmount();

        limit.setPeriodType(limitDto.periodType());
        limit.setCategory(limitDto.category());
        limit.setAmount(limitDto.amount());

        outboxService.save("Limit", limitId.toString(), "limit.changed",
                new LimitActivityEvent(userId, LimitActivityType.EDITED_LIMIT, limit.getPeriodType().name(), limit.getAmount(), oldLimitAmount, LocalDateTime.now()));

        return limitId;
    }

    @Transactional(readOnly = true)
    public List<LimitStatsDto> getLimitStats(Long userId) {
        List<Limit> limits = limitRepository.findAllActiveByUserId(userId);
        return limitCalculateService.calculateLimitStats(limits, userId, LocalDate.now());
    }

    @Transactional
    public void deleteLimit(Long userId, Long limitId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        Limit limit = limitManagerService.getLimitByUserIdOrThrow(userId, limitId);

        outboxService.save("Limit", limitId.toString(), "limit.changed",
                new LimitActivityEvent(userId, LimitActivityType.DELETED_LIMIT, limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));
        limitRepository.delete(limit);
    }

    private void throwIfLimitAlreadyExists(Long userId, LimitDto limitDto, Long editedLimitId) {
        Optional<Limit> existing = limitDto.category() == null ? limitRepository.findGeneralLimit(userId, limitDto.periodType())
                : limitRepository.findCategoryLimit(userId, limitDto.periodType(), limitDto.category());

        boolean duplicate = existing.isPresent() && !existing.get().getId().equals(editedLimitId);
        if (duplicate) {
            throw new EntityAlreadyExistsException(limitDto.category() == null ? "General limit already exists" : "Category limit already exists");
        }
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        List<Limit> limits = limitRepository.findAllByUserId(userId);
        limitRepository.deleteAll(limits);
        log.info("Deleted limit for userId={}", userId);
    }
}
