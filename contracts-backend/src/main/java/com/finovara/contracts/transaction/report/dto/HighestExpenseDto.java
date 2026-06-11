package com.finovara.contracts.transaction.report.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;

public record HighestExpenseDto(
        ExpenseCategory expenseCategory,
        BigDecimal amount
) {
}
