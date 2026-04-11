package com.finovara.finovarabackend.limit.dto;

import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitStatsDto(
        Long limitId,
        PeriodType periodType,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentage,
        LimitStatus status,
        LocalDate createdAt

) {
}
