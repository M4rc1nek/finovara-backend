package com.finovara.finovarabackend.expense.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseDTO(

        Long id,
        Long userId,

        @DecimalMin("0.01") @DecimalMax("5000000") BigDecimal amount,
        ExpenseCategory category,
        LocalDate createdAt,
        String description

) {
}
