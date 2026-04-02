package com.finovara.finovarabackend.accountactivity.limit.dto;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimitActivityDto(
        LimitActivityType limitActivityType,
        PeriodType periodType,
        BigDecimal amount,
        BigDecimal previousAmount,
        LocalDateTime date
) {
}
