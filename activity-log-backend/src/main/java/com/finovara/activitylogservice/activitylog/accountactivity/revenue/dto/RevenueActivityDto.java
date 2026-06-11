package com.finovara.activitylogservice.activitylog.accountactivity.revenue.dto;

import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueActivityDto(
        RevenueActivityType type,
        BigDecimal amount,
        BigDecimal previousAmount,
        RevenueCategory category,
        RevenueCategory previousCategory,
        LocalDateTime createdAt
) {
}
