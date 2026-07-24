package com.finovara.financeservice.sharedaccount.revenue.dto;

import com.finovara.contracts.model.transaction.RevenueCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SharedRevenueDto(
        Long id,

        @DecimalMin("1") @DecimalMax("999999") BigDecimal amount,
        RevenueCategory category,
        LocalDate createdAt,

        @Size(max = 100)
        String description,
        Long revenueCreatorId,
        String revenueCreatorUsername


) {
}
