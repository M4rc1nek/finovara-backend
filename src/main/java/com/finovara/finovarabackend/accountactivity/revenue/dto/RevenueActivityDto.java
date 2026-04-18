package com.finovara.finovarabackend.accountactivity.revenue.dto;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;

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
