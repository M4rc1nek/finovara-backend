package com.finovara.financeservice.settings.finances.recurring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.PeriodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseDto(
        Boolean enable,
        @DecimalMin("1") @DecimalMax("5000000") BigDecimal amount,
        ExpenseCategory expenseCategory,
        @NotNull PeriodType periodType,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextExecutionDate,
        String authorizationCode
) {
}
