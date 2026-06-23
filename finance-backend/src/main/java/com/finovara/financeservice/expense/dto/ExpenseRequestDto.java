package com.finovara.financeservice.expense.dto;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import jakarta.validation.Valid;

public record ExpenseRequestDto(
        @Valid ExpenseDto expenseDto,
        ConfirmPasswordDto confirmPasswordDto,
        @Valid CountQuantityLimitDto countQuantityLimitDto
) {
}
