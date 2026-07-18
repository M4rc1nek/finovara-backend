package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedLimitManagementService {
    private final SharedLimitRepository sharedLimitRepository;
    private final SharedLimitCalculateService sharedLimitCalculateService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final LimitExpensesValidator limitExpensesValidator;

    @Transactional
    public Long createSharedLimit(SharedLimitDto limitDto, Long userId) {
        limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(userId, limitDto);

        SharedAccountParticipantsResponse sharedAccountParticipantsResponse = sharedAccountParticipantsService.getParticipants(userId);
        if (limitDto.category() == null) {
            sharedLimitRepository.findGeneralLimit(userId, limitDto.periodType())
                    .ifPresent(limit -> {
                        throw new EntityAlreadyExistsException("General limit already exists");
                    });
        } else {
            sharedLimitRepository.findCategoryLimit(userId, limitDto.periodType(), limitDto.category())
                    .ifPresent(limit -> {
                        throw new EntityAlreadyExistsException("Category limit already exists");
                    });
        }

        SharedLimit limit = SharedLimit.builder()
                .periodType(limitDto.periodType())
                .category(limitDto.category())
                .amount(limitDto.amount())
                .isActive(true)
                .ownerId(sharedAccountParticipantsResponse.ownerId())
                .memberId(sharedAccountParticipantsResponse.memberId())
                .build();

        SharedLimit savedLimit = sharedLimitRepository.save(limit);

        return savedLimit.getId();
    }

    @Transactional
    public Long editSharedLimit(SharedLimitDto limitDto, Long limitId, Long userId) {
        SharedLimit limit = sharedLimitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));

        limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(userId, limitDto);

        limit.setPeriodType(limitDto.periodType());
        limit.setCategory(limitDto.category());
        limit.setAmount(limitDto.amount());

        sharedLimitRepository.save(limit);

        return limitId;
    }

    public List<SharedLimitStatsDto> getSharedLimitStats(Long userId) {
        List<SharedLimit> limits = sharedLimitRepository.findAllByUserId(userId);
        LocalDate today = LocalDate.now();

        return limits.stream()
                .map(limit ->
                        sharedLimitCalculateService.calculateLimitStats(userId, limit.getId(), today))
                .toList();
    }

    @Transactional
    public void deleteSharedLimit(Long userId, Long limitId) {
        SharedLimit limit = sharedLimitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));

        sharedLimitRepository.delete(limit);
    }

}