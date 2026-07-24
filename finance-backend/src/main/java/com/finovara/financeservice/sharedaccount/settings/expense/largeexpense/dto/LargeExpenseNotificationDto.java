package com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record LargeExpenseNotificationDto(
        Boolean largeExpenseNotificationEnabled,
        @DecimalMin("0.01") @DecimalMax("999999") BigDecimal largeExpenseNotificationThreshold
) {
}
