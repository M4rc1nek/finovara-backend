package com.finovara.reportservice.report.finances.calculate.categorypercentage.expense.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategoryPercentageDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}

