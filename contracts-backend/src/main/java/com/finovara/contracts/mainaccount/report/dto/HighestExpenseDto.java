package com.finovara.contracts.mainaccount.report.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record HighestExpenseDto(
        ExpenseCategory category,
        BigDecimal amount
) {
}