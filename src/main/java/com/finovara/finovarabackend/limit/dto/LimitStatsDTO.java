package com.finovara.finovarabackend.limit.dto;

import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.limit.model.LimitType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitStatsDTO(
        Long limitId,
        LimitType limitType,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentage,
        LimitStatus status,
        LocalDate date

) {
}
