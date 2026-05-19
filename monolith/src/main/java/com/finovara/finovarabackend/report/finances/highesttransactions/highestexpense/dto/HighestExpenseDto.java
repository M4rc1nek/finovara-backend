package com.finovara.finovarabackend.report.finances.highesttransactions.highestexpense.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;

import java.math.BigDecimal;

public record HighestExpenseDto(
        ExpenseCategory expenseCategory,
        BigDecimal amount
) {
}
