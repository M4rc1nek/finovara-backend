package com.finovara.finovarabackend.reports.finances.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;

import java.math.BigDecimal;

public record CategorySpendingDto(
        BigDecimal percentage,
        ExpenseCategory category
) {
}
