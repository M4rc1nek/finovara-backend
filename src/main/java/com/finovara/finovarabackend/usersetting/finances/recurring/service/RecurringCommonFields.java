package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringCommonFields(
        Boolean enable,
        BigDecimal amount,
        PeriodType periodType,
        LocalDate startDate
) {
}

