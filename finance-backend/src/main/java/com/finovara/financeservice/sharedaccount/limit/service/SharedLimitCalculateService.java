package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.mapper.SharedLimitMapper;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SharedLimitCalculateService {
    private final FinancialPeriodService financialPeriodService;
    private final SharedLimitRepository limitRepository;
    private final SharedLimitSpendingService sharedLimitSpendingService;
    private final SharedLimitMapper limitMapper;

    public SharedLimitStatsDto calculateLimitStats(SharedLimit limit, Long userId, LocalDate date) {
        BigDecimal spent = financialPeriodService.getSharedExpensesSum(userId, limit.getPeriodType(), limit.getCategory());
        return buildLimitStats(limit, spent, date);
    }

    public SharedLimitStatsDto calculateLimitStats(Long userId, Long limitId, LocalDate date) {
        SharedLimit limit = limitRepository.findByIdAndOwnerIdOrMemberId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active Limit not found"));

        return calculateLimitStats(limit, userId, date);
    }

    public List<SharedLimitStatsDto> calculateLimitStats(List<SharedLimit> limits,
                                                         SharedAccountParticipantsResponse participants, LocalDate date) {
        if (limits.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> spentByLimitId = sharedLimitSpendingService.calculateSpent(participants, limits, date);

        return limits.stream()
                .map(limit -> buildLimitStats(limit, spentByLimitId.getOrDefault(limit.getId(), BigDecimal.ZERO), date))
                .toList();
    }

    public SharedLimitStatsDto buildLimitStats(SharedLimit limit, BigDecimal spent, LocalDate date) {
        BigDecimal remaining = limit.getAmount().subtract(spent).max(BigDecimal.ZERO);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(spent, limit.getAmount());
        LimitStatus status = LimitStatus.fromPercentage(percentage);

        return limitMapper.mapLimitStatsToDto(limit, spent, remaining, percentage, status, date);
    }
}