package com.finovara.finovarabackend.usersetting.finances.recurring.dto;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringSettingsDto(
        Boolean enable,
        BigDecimal amount,
        RecurringType type,
        RevenueCategory revenueCategory,
        ExpenseCategory expenseCategory,
        PeriodType periodType,
        LocalDate startDate,
        LocalDate nextExecutionDate
) {
}
