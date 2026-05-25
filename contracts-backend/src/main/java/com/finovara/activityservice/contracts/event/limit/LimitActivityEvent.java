package com.finovara.activityservice.contracts.event.limit;

import com.finovara.activityservice.contracts.model.activity.LimitActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimitActivityEvent(
        Long userId,
        LimitActivityType type,
        String periodType,
        BigDecimal amount,
        BigDecimal previousAmount,
        LocalDateTime occurredAt
) {}