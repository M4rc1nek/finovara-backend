package com.finovara.financeservice.internal.report.util.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record MonthlyExpenseTotalByCategoryDto(
        ExpenseCategory category,
        BigDecimal amount
) {
}
