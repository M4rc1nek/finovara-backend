package com.finovara.financeservice.piggybank.goalplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoalPlannerCompletionSummaryDto(
        long durationDays,
        long durationHours,
        long durationMinutes,
        BigDecimal totalSaved,
        LocalDateTime completedOnTime

) {
}
