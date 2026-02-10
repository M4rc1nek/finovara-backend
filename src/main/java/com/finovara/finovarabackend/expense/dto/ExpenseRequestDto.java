package com.finovara.finovarabackend.expense.dto;

import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.util.service.user.dto.ConfirmPasswordDto;

public record ExpenseRequestDto(
        ExpenseDTO expenseDTO,
        ConfirmPasswordDto confirmPasswordDto,
        CountQuantityLimitDto countQuantityLimitDto
){
}
