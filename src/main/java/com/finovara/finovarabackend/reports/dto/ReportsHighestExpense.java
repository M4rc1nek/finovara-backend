package com.finovara.finovarabackend.reports.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;

import java.math.BigDecimal;

public record ReportsHighestExpense(
        ExpenseCategory expenseCategory,
        BigDecimal amount
) {
}
