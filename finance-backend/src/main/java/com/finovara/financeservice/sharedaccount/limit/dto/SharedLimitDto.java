package com.finovara.financeservice.sharedaccount.limit.dto;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.limit.model.LimitStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record LimitDto(
        Long id,

        PeriodType periodType,
        ExpenseCategory category,
        LimitStatus limitStatus,
        @DecimalMin(value = "1.0", message = "Amount must be at least 1")
        @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1 000 000")
        @Digits(integer = 7, fraction = 2)
        BigDecimal amount,

        Boolean isActive
) {
}
