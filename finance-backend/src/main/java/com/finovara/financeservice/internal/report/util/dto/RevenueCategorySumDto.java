package com.finovara.financeservice.internal.report.util.dto;

import com.finovara.contracts.util.model.RevenueCategory;

import java.math.BigDecimal;

public record RevenueCategorySumDto(RevenueCategory category, BigDecimal amount) {
}