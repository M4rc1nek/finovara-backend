package com.finovara.finovarabackend.report.finances.highestexpense.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;

import java.math.BigDecimal;

public record ReportsHighestExpense(
        ExpenseCategory expenseCategory,
        BigDecimal amount
) {
}
