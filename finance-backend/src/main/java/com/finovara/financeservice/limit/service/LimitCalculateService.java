package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.mapper.LimitMapper;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LimitCalculateService {
    private final FinancialPeriodService financialPeriodService;
    private final LimitManagerService limitManagerService;
    private final LimitSpendingService limitSpendingService;
    private final LimitMapper limitMapper;

    public LimitStatsDto calculateLimitStats(Limit limit, Long userId, LocalDate date) {
        BigDecimal spent = financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory());
        return buildLimitStats(limit, spent, date);
    }

    public LimitStatsDto calculateLimitStats(Long userId, Long limitId, LocalDate date) {
        Limit limit = limitManagerService.getLimitByUserIdOrThrow(userId, limitId);
        return calculateLimitStats(limit, userId, date);
    }

    public List<LimitStatsDto> calculateLimitStats(List<Limit> limits, Long userId, LocalDate date) {
        Map<Long, BigDecimal> spentByLimitId = limitSpendingService.calculateSpent(userId, limits, date);

        return limits.stream()
                .map(limit -> buildLimitStats(limit, spentByLimitId.get(limit.getId()), date))
                .toList();
    }

    public Map<Long, List<LimitStatsDto>> calculateLimitStatsForUsers(
            Map<Long, List<Limit>> limitsByUserId, LocalDate date) {
        Map<Long, Map<Long, BigDecimal>> spentByUserAndLimit = limitSpendingService.calculateSpent(limitsByUserId, date);
        Map<Long, List<LimitStatsDto>> statsByUserId = new HashMap<>();
        for (Map.Entry<Long, List<Limit>> entry : limitsByUserId.entrySet()) {
            Map<Long, BigDecimal> userSpent = spentByUserAndLimit.getOrDefault(entry.getKey(), Map.of());
            List<LimitStatsDto> userStats = entry.getValue().stream()
                    .map(limit -> buildLimitStats(limit,
                            userSpent.getOrDefault(limit.getId(), BigDecimal.ZERO), date))
                    .toList();
            statsByUserId.put(entry.getKey(), userStats);
        }
        return statsByUserId;
    }

    public LimitStatsDto buildLimitStats(Limit limit, BigDecimal spent, LocalDate date) {
        BigDecimal remaining = limit.getAmount().subtract(spent).max(BigDecimal.ZERO);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(spent, limit.getAmount());
        LimitStatus status = LimitStatus.fromPercentage(percentage);

        return limitMapper.mapLimitStatsToDto(limit, spent, remaining, percentage, status, date);
    }
}