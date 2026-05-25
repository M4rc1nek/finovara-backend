package com.finovara.corebackend.report.finances.categorypercentage.expense.dto;

import com.finovara.activityservice.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategoryPercentageDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}

