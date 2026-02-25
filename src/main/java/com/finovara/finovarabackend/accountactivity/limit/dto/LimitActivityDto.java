package com.finovara.finovarabackend.accountactivity.limit.dto;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.limit.model.LimitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimitActivityDto(
        LimitActivityType limitActivityType,
        LimitType limitType,
        BigDecimal amount,
        BigDecimal previousAmount,
        LocalDateTime date
) {
}
