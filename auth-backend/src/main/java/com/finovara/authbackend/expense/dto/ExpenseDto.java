package com.finovara.authbackend.expense.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseDto(

        Long id,
        Long userId,

        @DecimalMin("0.01") @DecimalMax("5000000") BigDecimal amount,
        ExpenseCategory category,
        LocalDate createdAt,
        String description

) {
}
