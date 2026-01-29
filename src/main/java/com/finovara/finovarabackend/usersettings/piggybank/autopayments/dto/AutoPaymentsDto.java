package com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto;

import java.math.BigDecimal;

public record AutoPaymentsDto(
        Long piggyBankId,
        Boolean isAutomationActive,
        BigDecimal percentage
) {
}
