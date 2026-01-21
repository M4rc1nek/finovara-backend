package com.finovara.finovarabackend.limit.service;

import com.finovara.finovarabackend.exception.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.dto.LimitStatsDTO;
import com.finovara.finovarabackend.limit.mapper.LimitMapper;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class LimitService {
    private final SpentInPeriodService spentInPeriodService;
    private final LimitRepository limitRepository;
    private final LimitMapper limitMapper;

    public LimitStatsDTO calculateLimitStats(Long userId, Long limitId, LocalDate date) {
        Limit limit = limitRepository.findByIdAndUserAssignedId(userId, limitId)
                .orElseThrow(() -> new ActiveLimitNotFoundException("Active Limit not found"));

        BigDecimal spent = switch (limit.getLimitType()) {
            case DAILY -> spentInPeriodService.getSpentToday(userId);
            case WEEKLY -> spentInPeriodService.getSpentWeekly(userId);
            case MONTHLY -> spentInPeriodService.getSpentMonthly(userId);
        };

        BigDecimal remaining = limit.getAmount().subtract(spent);

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        BigDecimal percentage = calculatePercentage(spent, limit.getAmount());
        LimitStatus status = determineStatus(percentage);

        return limitMapper.mapLimitStatsToDTO(limit, spent, remaining, percentage, status, date);

    }

    private BigDecimal calculatePercentage(BigDecimal spent, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentSpent = (spent != null) ? spent : BigDecimal.ZERO;

        return currentSpent
                .multiply(BigDecimal.valueOf(100))
                .divide(limit, 2, RoundingMode.HALF_UP);

    }

    private LimitStatus determineStatus(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return LimitStatus.HIGH;
        if (percentage.compareTo(BigDecimal.valueOf(50)) >= 0) return LimitStatus.MEDIUM;
        if (percentage.compareTo(BigDecimal.valueOf(25)) >= 0) return LimitStatus.LOW;
        return LimitStatus.NONE;
    }

}
