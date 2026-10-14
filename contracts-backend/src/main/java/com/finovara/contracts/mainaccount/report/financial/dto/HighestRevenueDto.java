package com.finovara.contracts.mainaccount.report.financial.dto;

import com.finovara.contracts.util.model.RevenueCategory;

import java.math.BigDecimal;

public record HighestRevenueDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
