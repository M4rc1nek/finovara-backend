package com.finovara.financeservice.revenue.suggestions.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseSuggestionDto(
        ExpenseCategory category,
        BigDecimal amount
) {
}
