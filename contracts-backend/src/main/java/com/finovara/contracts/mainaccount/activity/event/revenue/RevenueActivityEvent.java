package com.finovara.contracts.mainaccount.activity.event.revenue;

import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.util.model.RevenueCategory;

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