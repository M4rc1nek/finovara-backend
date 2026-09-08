package com.finovara.financeservice.revenue.suggestions.dto;

import com.finovara.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;

public record RevenueSuggestionDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
