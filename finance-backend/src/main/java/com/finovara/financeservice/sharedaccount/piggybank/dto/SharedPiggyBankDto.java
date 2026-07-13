package com.finovara.financeservice.sharedaccount.piggybank.dto;

import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SharedPiggyBankDto(
        Long id,

        @Size(min = 3, max = 15)
        @NotBlank
        String name,

        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "10000000", message = "Amount must not exceed 10 000 000")
        BigDecimal amount,
        LocalDate createdAt,

        PiggyBankGoalType goalType,
        BigDecimal goalAmount,

        Double progress
) {
}
