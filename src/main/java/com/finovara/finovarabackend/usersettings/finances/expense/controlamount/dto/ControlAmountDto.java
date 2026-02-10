package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto;

import java.math.BigDecimal;

public record ControlAmountDto(
        Boolean expenseAmountThresholdEnabled,
        BigDecimal blockedAmount
) {
}
