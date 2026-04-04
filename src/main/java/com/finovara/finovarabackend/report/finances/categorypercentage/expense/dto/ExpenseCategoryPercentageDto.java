package com.finovara.finovarabackend.report.finances.categorypercentage.expense.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategoryPercentageDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}

