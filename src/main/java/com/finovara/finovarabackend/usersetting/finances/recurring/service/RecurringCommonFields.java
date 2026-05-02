package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringCommonFields(
        Boolean enable,
        @DecimalMin("1") @DecimalMax("5000000") BigDecimal amount,
        PeriodType periodType,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate
) {
}

