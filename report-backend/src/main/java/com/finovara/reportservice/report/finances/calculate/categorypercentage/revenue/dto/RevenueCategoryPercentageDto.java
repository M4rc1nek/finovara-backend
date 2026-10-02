package com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto;

import com.finovara.contracts.util.model.RevenueCategory;

import java.math.BigDecimal;

public record RevenueCategoryPercentageDto(
        BigDecimal percentage,
        RevenueCategory category
) {
}

