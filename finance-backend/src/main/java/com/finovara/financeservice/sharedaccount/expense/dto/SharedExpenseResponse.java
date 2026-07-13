package com.finovara.financeservice.sharedaccount.dto.expense;

public record SharedExpenseResponse(
        Long expenseId,
        Long userId,
        String username
) {
}
