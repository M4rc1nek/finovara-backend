package com.finovara.finovarabackend.report.finances.categorypercentage.revenue.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;

import java.math.BigDecimal;

public record RevenueCategoryPercentageDto(
        BigDecimal percentage,
        RevenueCategory category
) {
}

