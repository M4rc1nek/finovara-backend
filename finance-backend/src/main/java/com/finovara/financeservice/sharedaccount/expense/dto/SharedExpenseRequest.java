package com.finovara.financeservice.sharedaccount.expense.dto;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;

public record SharedExpenseRequest(
       @Valid SharedExpenseDto sharedExpenseDto,
       ConfirmPasswordDto confirmPasswordDto
) {
}
