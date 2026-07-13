package com.finovara.reportservice.util.dto.financial;

import java.math.BigDecimal;

public record FinancialCategoryPercentageDto(
        String category,
        BigDecimal percentage
) {
}

