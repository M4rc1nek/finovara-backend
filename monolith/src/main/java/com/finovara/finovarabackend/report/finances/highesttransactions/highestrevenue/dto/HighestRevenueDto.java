package com.finovara.finovarabackend.report.finances.highesttransactions.highestrevenue.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;

import java.math.BigDecimal;

public record HighestRevenueDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
