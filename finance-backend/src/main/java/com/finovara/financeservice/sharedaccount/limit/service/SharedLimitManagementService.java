package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.LimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final OutboxService outboxService;

    @Transactional
    public Long createLimit(SharedLimitDto limitDto, Long userId) {
        if (limitDto.category() == null) {
            limitRepository.findGeneralLimit(userId, limitDto.periodType())
                    .ifPresent(limit -> { throw new EntityAlreadyExistsException("General limit already exists"); });
        } else {
            limitRepository.findCategoryLimit(userId, limitDto.periodType(), limitDto.category())
                    .ifPresent(limit -> { throw new EntityAlreadyExistsException("Category limit already exists"); });
        }

        SharedLimit limit = SharedLimit.builder()
                .periodType(limitDto.periodType())
                .category(limitDto.category())
                .amount(limitDto.amount())
                .isActive(true)
                .userId(userId)
                .build();

        SharedLimit savedLimit = limitRepository.save(limit);
        outboxService.save("Limit", savedLimit.getId().toString(), "activity.limit",
                new LimitActivityEvent(userId, LimitActivityType.ADDED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));

        return savedLimit.getId();
    }

    @Transactional
    public Long editLimit(SharedLimitDto limitDto, Long limitId, Long userId) {
        SharedLimit limit = limitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));

        if (!limit.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Active Limit not found for this user");
        }

        BigDecimal oldLimitAmount = limit.getAmount();

        limit.setPeriodType(limitDto.periodType());
        limit.setCategory(limitDto.category());
        limit.setAmount(limitDto.amount());

        limitRepository.save(limit);
        outboxService.save("Limit", limitId.toString(), "activity.limit",
                new LimitActivityEvent(userId, LimitActivityType.EDITED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), oldLimitAmount, LocalDateTime.now()));

        return limitId;
    }

    public List<SharedLimitStatsDto> getLimitStats(Long userId) {
        List<SharedLimit> limits = limitRepository.findAllByUserId(userId);
        LocalDate today = LocalDate.now();

        return limits.stream()
                .map(limit ->
                        limitCalculateService.calculateLimitStats(userId, limit.getId(), today))
                .toList();
    }

    @Transactional
    public void deleteLimit(Long userId, Long limitId) {
        SharedLimit limit = limitRepository.findByIdAndUserId(userId, limitId)
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
