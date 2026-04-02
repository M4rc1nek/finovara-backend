package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringRevenueDto(
        Boolean recurringRevenueEnable,
        BigDecimal amount,
        RevenueCategory category,
        PeriodType periodType,
        LocalDate startDate,
        LocalDate nextExecutionDate
) {
}
