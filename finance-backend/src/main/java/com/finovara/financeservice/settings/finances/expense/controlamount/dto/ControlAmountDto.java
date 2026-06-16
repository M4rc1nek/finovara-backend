package com.finovara.financeservice.settings.finances.expense.controlamount.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record ControlAmountDto(
        Boolean expenseAmountThresholdEnabled,
       @DecimalMin("1") @DecimalMax("1000000") BigDecimal blockedAmount
) {
}
