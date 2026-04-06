package com.finovara.finovarabackend.expense.dto;

import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;

public record ExpenseRequestDto(
        @Valid ExpenseDTO expenseDTO,
        ConfirmPasswordDto confirmPasswordDto,
        @Valid CountQuantityLimitDto countQuantityLimitDto
) {
}
