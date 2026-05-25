package com.finovara.corebackend.report.finances.highesttransactions.highestrevenue.dto;

import com.finovara.activityservice.contracts.model.transaction.RevenueCategory;

import java.math.BigDecimal;

public record HighestRevenueDto(
        RevenueCategory category,
        BigDecimal amount
) {
}
