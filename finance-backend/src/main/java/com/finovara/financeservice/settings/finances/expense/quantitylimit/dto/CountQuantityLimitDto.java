package com.finovara.financeservice.settings.finances.expense.quantitylimit.dto;

import com.finovara.contracts.model.PeriodType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CountQuantityLimitDto(
        Boolean expenseCountLimitEnabled,
        PeriodType periodType,
        @Min(1) @Max(100) int numberOfQuantityLimit,
        String authorizationCode
) {
}
