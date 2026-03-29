package com.finovara.finovarabackend.report.finances.categoryspending.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;

import java.math.BigDecimal;

public record CategorySpendingDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}
