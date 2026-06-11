package com.finovara.contracts.transaction.report.dto;

import com.finovara.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;

public record HighestRevenueDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
