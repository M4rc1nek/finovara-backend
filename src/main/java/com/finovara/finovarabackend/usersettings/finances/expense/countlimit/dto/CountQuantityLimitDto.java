package com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto;

import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CountQuantityLimitDto(
        Boolean expenseCountLimitEnabled,
        CountQuantityLimitStrategy countQuantityLimitStrategy,
        @Min(1) @Max(100) int numberOfQuantityLimit
) {
}
