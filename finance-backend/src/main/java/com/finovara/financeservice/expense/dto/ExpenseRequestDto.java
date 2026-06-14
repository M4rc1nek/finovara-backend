package com.finovara.authbackend.expense.dto;

import com.finovara.authbackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;

public record ExpenseRequestDto(
        @Valid ExpenseDto expenseDto,
        ConfirmPasswordDto confirmPasswordDto,
        @Valid CountQuantityLimitDto countQuantityLimitDto
) {
}
