package com.finovara.finovarabackend.piggybank.dto;

import com.finovara.finovarabackend.piggybank.model.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PiggyBankDTO(
        Long id,
        Long userId,

        @Size(min = 3, max = 15)
        @NotBlank
        String name,


        BigDecimal amount,
        LocalDate createdAt,

        GoalType goalType,
        BigDecimal goalAmount,

        Double progress,
        Boolean goalCompleted


) {
}
