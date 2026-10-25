package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedLimitManagementService {
    private final SharedLimitRepository sharedLimitRepository;
    private final SharedLimitCalculateService sharedLimitCalculateService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SharedWalletService sharedWalletService;
    private final LimitExpensesValidator limitExpensesValidator;
    private final OutboxService outboxService;

    @Transactional
    public Long createSharedLimit(SharedLimitDto limitDto, Long userId) {
        sharedWalletService.lockWalletForUpdate(userId);

        limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(userId, limitDto);
        throwIfLimitAlreadyExists(userId, limitDto, null);

        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);

        SharedLimit savedLimit = sharedLimitRepository.save(SharedLimit.builder()
                .periodType(limitDto.periodType())
                .category(limitDto.category())
                .amount(limitDto.amount())
                .isActive(true)
                .ownerId(participants.ownerId())
                .memberId(participants.memberId())
                .build());

        logActivity(participants, userId, savedLimit.getId(), SharedAccountActivityLogType.LIMIT_CREATED);

        return savedLimit.getId();
    }

    @Transactional
    public Long editSharedLimit(SharedLimitDto limitDto, Long limitId, Long userId) {
        sharedWalletService.lockWalletForUpdate(userId);
        SharedLimit limit = getSharedLimitByUserIdOrThrow(userId, limitId);

        limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(userId, limitDto);
        throwIfLimitAlreadyExists(userId, limitDto, limitId);

        limit.setPeriodType(limitDto.periodType());
        limit.setCategory(limitDto.category());
        limit.setAmount(limitDto.amount());

        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, limitId, SharedAccountActivityLogType.LIMIT_EDITED);

        return limitId;
    }

    @Transactional(readOnly = true)
    public List<SharedLimitStatsDto> getSharedLimitStats(Long userId) {
        List<SharedLimit> limits = sharedLimitRepository.findAllByUserId(userId);
        if (limits.isEmpty()) {
            return List.of();
        }
        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
        return sharedLimitCalculateService.calculateLimitStats(limits, participants, LocalDate.now());
    }

    @Transactional
    public void deleteSharedLimit(Long userId, Long limitId) {
        SharedLimit limit = getSharedLimitByUserIdOrThrow(userId, limitId);

        sharedLimitRepository.delete(limit);
        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, limitId, SharedAccountActivityLogType.LIMIT_DELETED);
    }

    private void throwIfLimitAlreadyExists(Long userId, SharedLimitDto limitDto, Long editedLimitId) {
        Optional<SharedLimit> existing = limitDto.category() == null
                ? sharedLimitRepository.findGeneralLimit(userId, limitDto.periodType())
                : sharedLimitRepository.findCategoryLimit(userId, limitDto.periodType(), limitDto.category());

        boolean duplicate = existing.isPresent() && !existing.get().getId().equals(editedLimitId);
        if (duplicate) {
            throw new EntityAlreadyExistsException(limitDto.category() == null
                    ? "General limit already exists" : "Category limit already exists");
        }
    }

    private void logActivity(SharedAccountParticipantsResponse participants, Long userId, Long limitId, SharedAccountActivityLogType type) {
        outboxService.save("SharedAccountLimit", limitId.toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, limitId, type, LocalDateTime.now()));
    }

    private SharedLimit getSharedLimitByUserIdOrThrow(Long userId, Long limitId) {
        return sharedLimitRepository.findByIdAndOwnerIdOrMemberId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Shared limit not found"));
    }
}