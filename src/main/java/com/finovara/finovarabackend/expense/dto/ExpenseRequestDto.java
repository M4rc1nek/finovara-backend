package com.finovara.finovarabackend.expense.dto;

import com.finovara.finovarabackend.util.service.user.dto.ConfirmPasswordDto;

public record ExpenseRequestDto(
        ExpenseDTO expenseDTO,
        ConfirmPasswordDto confirmPasswordDto
){
}
