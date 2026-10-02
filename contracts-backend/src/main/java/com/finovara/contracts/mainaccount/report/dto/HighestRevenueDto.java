package com.finovara.contracts.transaction.report.dto;

import com.finovara.contracts.util.model.RevenueCategory;

import java.math.BigDecimal;

public record HighestRevenueDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
