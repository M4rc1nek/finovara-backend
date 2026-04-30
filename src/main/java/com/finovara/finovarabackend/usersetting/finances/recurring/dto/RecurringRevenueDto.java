package com.finovara.finovarabackend.usersetting.finances.recurring.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringRevenueDto(
        Boolean enable,
        @DecimalMin("1") @DecimalMax("5000000") @DefaultValue("1") BigDecimal amount,
        RevenueCategory revenueCategory,
        @NotNull PeriodType periodType,
        LocalDate startDate,
        LocalDate nextExecutionDate

) {
}
