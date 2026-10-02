package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record SharedExpenseCategoryPercentageDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}

