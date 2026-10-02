package com.finovara.financeservice.expense.suggestions.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseSuggestionDto(
        ExpenseCategory category,
        BigDecimal amount
) {
}
