package com.finovara.corebackend.limit.dto;

import com.finovara.corebackend.limit.model.LimitStatus;
import com.finovara.activityservice.contracts.model.PeriodType;

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
