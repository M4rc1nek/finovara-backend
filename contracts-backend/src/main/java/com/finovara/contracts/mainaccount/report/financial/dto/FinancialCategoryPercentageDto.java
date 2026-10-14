package com.finovara.contracts.mainaccount.report.financial.dto;

import java.math.BigDecimal;

public record FinancialCategoryPercentageDto(
        String category,
        BigDecimal percentage
) {
}

