package com.finovara.activitylogservice.activitylog.mainaccount.limit.dto;

import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;
import com.finovara.contracts.util.PeriodType;

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
