package com.finovara.finovarabackend.revenue.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueDTO(
        Long id,
        Long userId,

        @DecimalMin("1") @DecimalMax("5000000") BigDecimal amount,
        RevenueCategory category,
        LocalDate createdAt,
        String description


) {
}
