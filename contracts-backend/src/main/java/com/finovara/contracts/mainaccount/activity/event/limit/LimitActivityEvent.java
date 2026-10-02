package com.finovara.contracts.mainaccount.activity.event.limit;

import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;

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