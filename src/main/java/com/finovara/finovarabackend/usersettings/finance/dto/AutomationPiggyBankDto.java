package com.finovara.finovarabackend.usersettings.finance.dto;

import java.math.BigDecimal;

public record AutomationPiggyBankDto(
        Boolean isAutomationActive,
        BigDecimal percentage
) {
}
