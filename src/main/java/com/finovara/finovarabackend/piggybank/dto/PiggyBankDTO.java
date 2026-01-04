package com.finovara.finovarabackend.piggybank.dto;

import com.finovara.finovarabackend.piggybank.model.GoalType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PiggyBankDTO(
        Long id,
        Long userId,

        String name,
        BigDecimal amount,
        LocalDate createdAt,

        GoalType goalType,
        BigDecimal goalAmount,

        Double progress,
        Boolean goalCompleted


) {
}
