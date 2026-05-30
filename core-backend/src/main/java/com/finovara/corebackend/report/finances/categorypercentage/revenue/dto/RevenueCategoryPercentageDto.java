package com.finovara.corebackend.report.finances.categorypercentage.revenue.dto;

import com.finovara.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;

public record RevenueCategoryPercentageDto(
        BigDecimal percentage,
        RevenueCategory category
) {
}

