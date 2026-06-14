package com.finovara.authbackend.limit.dto;

import com.finovara.authbackend.limit.model.LimitStatus;
import com.finovara.contracts.model.PeriodType;

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
