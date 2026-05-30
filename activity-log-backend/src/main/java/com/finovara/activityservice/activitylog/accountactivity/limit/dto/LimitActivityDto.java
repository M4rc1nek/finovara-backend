package com.finovara.activityservice.activitylog.accountactivity.limit.dto;

import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.contracts.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimitActivityDto(
        LimitActivityType limitActivityType,
        PeriodType periodType,
        BigDecimal amount,
        BigDecimal previousAmount,
        LocalDateTime createdAt
) {
}
