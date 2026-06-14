package com.finovara.authbackend.usersetting.finances.recurring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringRevenueDto(
        Boolean enable,
        @DecimalMin("1") @DecimalMax("5000000") BigDecimal amount,
        RevenueCategory revenueCategory,
        @NotNull PeriodType periodType,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        LocalDate nextExecutionDate

) {
}
