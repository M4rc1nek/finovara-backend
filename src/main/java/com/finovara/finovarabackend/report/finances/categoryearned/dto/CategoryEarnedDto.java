package com.finovara.finovarabackend.report.finances.categoryearned.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;

import java.math.BigDecimal;

public record CategoryEarnedDto(
        BigDecimal percentage,
        RevenueCategory category
) {
}
