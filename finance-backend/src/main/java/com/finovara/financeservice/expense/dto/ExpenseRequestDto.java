package com.finovara.financeservice.expense.dto;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import jakarta.validation.Valid;

public record ExpenseRequestDto(
        @Valid ExpenseDto expenseDto,
        ConfirmPasswordDto confirmPasswordDto,
        ConfirmAuthorizationCodeDto confirmAuthorizationCodeDto,
        @Valid CountQuantityLimitDto countQuantityLimitDto
) {
}
