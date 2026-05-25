package com.finovara.corebackend.piggybank.dto;

import com.finovara.activityservice.contracts.model.transaction.PiggyBankGoalType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PiggyBankDto(
        Long id,
        Long userId,

        @Size(min = 3, max = 15)
        @NotBlank
        String name,

        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "10000000", message = "Amount must not exceed 10 000 000")
        BigDecimal amount,
        LocalDate createdAt,

        PiggyBankGoalType goalType,
        BigDecimal goalAmount,

        Double progress,
        Boolean goalCompleted

) {
}
