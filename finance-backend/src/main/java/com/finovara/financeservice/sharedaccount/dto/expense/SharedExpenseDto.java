package com.finovara.financeservice.sharedaccount.dto.expense;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SharedExpenseDto(

        Long id,

        @DecimalMin("0.01") @DecimalMax("999999") BigDecimal amount,
        ExpenseCategory category,
        LocalDate createdAt,
        @Size(max = 100)
        String description,
        Long expenseCreatorId,
        String expenseCreatorUsername

) {
}
