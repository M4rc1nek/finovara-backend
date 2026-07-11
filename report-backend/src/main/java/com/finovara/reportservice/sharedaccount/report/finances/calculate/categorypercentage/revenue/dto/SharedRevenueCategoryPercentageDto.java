package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.dto;

import com.finovara.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;

public record SharedRevenueCategoryPercentageDto(
        BigDecimal percentage,
        RevenueCategory category
) {
}

