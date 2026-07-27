package com.finovara.financeservice.piggybank.goalplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalPlannerDto(
        Long id,

        @NotNull
        Long piggyBankId,

        @Size(min = 3, max = 15)
        @NotBlank String topic,

        BigDecimal goalAmount,

        LocalDate targetDate,
        LocalDateTime createdAt
) {
}