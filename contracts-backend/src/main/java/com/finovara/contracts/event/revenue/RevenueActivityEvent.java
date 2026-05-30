package com.finovara.contracts.event.revenue;

import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueActivityEvent(
        Long userId,
        RevenueActivityType type,
        BigDecimal amount,
        RevenueCategory category,
        BigDecimal previousAmount,
        RevenueCategory previousCategory,
        LocalDateTime occurredAt
) {}