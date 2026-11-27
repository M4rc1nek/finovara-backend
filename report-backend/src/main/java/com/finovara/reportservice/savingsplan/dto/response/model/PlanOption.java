package com.finovara.reportservice.savingsplan.dto.response.model;

import java.math.BigDecimal;

public record  PlanOption(
        OptionType type,
        BigDecimal targetAmount,
        Integer months,
        BigDecimal monthlySavings,
        String message
) {
}
