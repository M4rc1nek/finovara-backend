package com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AutoPaymentsDto(
        Boolean isAutomationActive,
        @DecimalMin("1") @DecimalMax("100") BigDecimal percentage
) {
}
