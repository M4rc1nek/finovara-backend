package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.mapper.SharedLimitMapper;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class SharedLimitCalculateService {
    private final FinancialPeriodService financialPeriodService;
    private final SharedLimitRepository limitRepository;
    private final SharedLimitMapper limitMapper;

    public SharedLimitStatsDto calculateLimitStats(SharedLimit limit, Long userId, LocalDate date) {
        BigDecimal spent = financialPeriodService.getSharedExpensesSum(userId, limit.getPeriodType(), limit.getCategory());

        BigDecimal remaining = limit.getAmount().subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        BigDecimal percentage = CalculatePercentage.calculatePercentage(spent, limit.getAmount());
        LimitStatus status = determineStatus(percentage);

        return limitMapper.mapLimitStatsToDto(limit, spent, remaining, percentage, status, date);
    }

    public SharedLimitStatsDto calculateLimitStats(Long userId, Long limitId, LocalDate date) {
        SharedLimit limit = limitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active Limit not found"));

        return calculateLimitStats(limit, userId, date);
    }

    private LimitStatus determineStatus(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return LimitStatus.HIGH;
        if (percentage.compareTo(BigDecimal.valueOf(50)) >= 0) return LimitStatus.MEDIUM;
        if (percentage.compareTo(BigDecimal.valueOf(25)) >= 0) return LimitStatus.LOW;
        return LimitStatus.NONE;
    }

}
