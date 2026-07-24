package com.finovara.financeservice.sharedaccount.limit.dto;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.limit.model.LimitStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SharedLimitStatsDto(
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
