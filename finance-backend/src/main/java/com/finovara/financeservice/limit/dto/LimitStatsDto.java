package com.finovara.financeservice.limit.dto;

import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.contracts.util.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitStatsDto(
        Long limitId,
        PeriodType periodType,
        ExpenseCategory category,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentage,
        LimitStatus status,
        LocalDate createdAt

) {
}
