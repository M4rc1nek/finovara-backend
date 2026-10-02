package com.finovara.financeservice.revenue.suggestions.dto;

import com.finovara.contracts.util.model.RevenueCategory;

import java.math.BigDecimal;

public record RevenueSuggestionDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
