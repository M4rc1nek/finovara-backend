package com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto;

import java.math.BigDecimal;

public record AutomationPiggyBankDto(
        Long piggyBankId,
        Boolean isAutomationActive,
        BigDecimal percentage
) {
}
