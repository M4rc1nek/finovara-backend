package com.finovara.financeservice.piggybank.goalplanner.dto;

import java.math.BigDecimal;

public record GoalPlannerSummaryDto(
        BigDecimal dailyInstallment,
        BigDecimal weeklyInstallment,
        BigDecimal monthlyInstallment,
        long daysUntilTarget
) {}