package com.finovara.contracts.mainaccount.report.savingsplan.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SavingsPlanMonthDto(
        LocalDate month,
        BigDecimal revenue,
        Map<ExpenseCategory, BigDecimal> expenses
) {
}
