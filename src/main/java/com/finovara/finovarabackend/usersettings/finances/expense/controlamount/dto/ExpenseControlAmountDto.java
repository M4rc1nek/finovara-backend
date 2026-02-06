package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto;

import java.math.BigDecimal;

public record ExpenseControlAmountDto(
        Long expenseId,
        Boolean expenseAmountThresholdEnabled,
        BigDecimal blockedAmount
) {
}
