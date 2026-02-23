package com.finovara.finovarabackend.expense.dto;

import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;

public record ExpenseRequestDto(
        ExpenseDTO expenseDTO,
        ConfirmPasswordDto confirmPasswordDto,
        CountQuantityLimitDto countQuantityLimitDto
){
}
