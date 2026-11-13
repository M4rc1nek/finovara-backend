package com.finovara.financeservice.revenue.dto;

import com.finovara.contracts.util.model.RevenueCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueDto(
        Long id,
        Long userId,

        @DecimalMin("1") @DecimalMax("999999") BigDecimal amount,
        RevenueCategory category,
        LocalDate createdAt,
        @Size(max = 500) String description,
        String authorizationCode,
        String riskVerificationSourceEventId


) {
}
