package com.finovara.reportservice.report.finances.categorypercentage.expense.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategoryPercentageDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}

