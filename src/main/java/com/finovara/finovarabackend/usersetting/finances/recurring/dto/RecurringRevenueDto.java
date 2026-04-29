package com.finovara.finovarabackend.usersetting.finances.recurring.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringRevenueDto(
        Boolean enable,
        BigDecimal amount,
        RevenueCategory revenueCategory,
        @NotNull PeriodType periodType,
        LocalDate startDate,
        LocalDate nextExecutionDate

) {
}
