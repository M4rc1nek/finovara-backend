package com.finovara.finovarabackend.limit.service;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.mapper.LimitMapper;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.util.service.calculate.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class LimitCalculateService {
    private final FinancialPeriodService financialPeriodService;
    private final LimitRepository limitRepository;
    private final LimitMapper limitMapper;

    public LimitStatsDto calculateLimitStats(Long userId, Long limitId, LocalDate date) {
        Limit limit = limitRepository.findByIdAndUserAssignedId(userId, limitId)
                .orElseThrow(() -> new ActiveLimitNotFoundException("Active Limit not found"));

        BigDecimal spent = financialPeriodService.getExpensesSum(userId, limit.getPeriodType());

        BigDecimal remaining = limit.getAmount().subtract(spent);

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        BigDecimal percentage = CalculatePercentage.calculatePercentage(spent, limit.getAmount());
        LimitStatus status = determineStatus(percentage);

        return limitMapper.mapLimitStatsToDto(limit, spent, remaining, percentage, status, date);
    }

    private LimitStatus determineStatus(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return LimitStatus.HIGH;
        if (percentage.compareTo(BigDecimal.valueOf(50)) >= 0) return LimitStatus.MEDIUM;
        if (percentage.compareTo(BigDecimal.valueOf(25)) >= 0) return LimitStatus.LOW;
        return LimitStatus.NONE;
    }

}
