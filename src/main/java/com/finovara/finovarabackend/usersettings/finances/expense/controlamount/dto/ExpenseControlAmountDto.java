package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto;

import java.math.BigDecimal;

public record ExpenseControlAmountDto(
        Boolean expenseAmountThresholdEnabled,
        BigDecimal blockedAmount
) {
}
