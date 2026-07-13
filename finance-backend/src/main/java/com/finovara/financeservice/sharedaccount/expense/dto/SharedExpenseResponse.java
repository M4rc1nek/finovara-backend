package com.finovara.financeservice.sharedaccount.expense.dto;

public record SharedExpenseResponse(
        Long expenseId,
        Long userId,
        String username
) {
}
