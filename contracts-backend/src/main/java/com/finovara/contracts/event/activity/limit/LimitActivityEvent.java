package com.finovara.contracts.event.activity.limit;

import com.finovara.contracts.model.activity.LimitActivityType;

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