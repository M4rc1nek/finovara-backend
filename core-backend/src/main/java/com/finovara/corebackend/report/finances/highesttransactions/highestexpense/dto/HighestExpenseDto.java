package com.finovara.corebackend.report.finances.highesttransactions.highestexpense.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;

public record HighestExpenseDto(
        ExpenseCategory expenseCategory,
        BigDecimal amount
) {
}
