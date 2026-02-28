package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.model.RecurringStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringRevenueDto(
        Boolean recurringRevenueEnable,
        BigDecimal amount,
        RevenueCategory category,
        RecurringStrategy strategy,
        LocalDate startDate,
        LocalDate nextExecutionDate
) {
}
