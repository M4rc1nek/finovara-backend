package com.finovara.financeservice.piggybank.goalplanner.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalPlanner(
        @Size(min = 3, max = 15)
        @NotBlank String topic,

        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "10000000", message = "Amount must not exceed 10 000 000")
        BigDecimal goalAmount,

        LocalDate targetDate,
        LocalDateTime createdAt
) {
}
