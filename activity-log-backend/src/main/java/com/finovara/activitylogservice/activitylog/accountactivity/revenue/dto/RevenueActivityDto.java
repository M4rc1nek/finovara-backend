package com.finovara.activitylogservice.activitylog.accountactivity.revenue.dto;

import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.util.model.RevenueCategory;

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
