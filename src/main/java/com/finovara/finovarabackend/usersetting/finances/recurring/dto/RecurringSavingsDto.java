package com.finovara.finovarabackend.usersetting.finances.recurring.dto;

import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringSavingsDto(
        Boolean enable,
        BigDecimal amount,
        Long piggyBankId,
        @NotNull PeriodType periodType,
        LocalDate startDate,
        LocalDate nextExecutionDate

) {
}
