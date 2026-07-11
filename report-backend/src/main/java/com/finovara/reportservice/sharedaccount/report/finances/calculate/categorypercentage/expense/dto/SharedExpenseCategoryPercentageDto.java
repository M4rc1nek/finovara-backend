package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;

public record SharedExpenseCategoryPercentageDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}

