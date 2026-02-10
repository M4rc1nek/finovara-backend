package com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto;

import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.model.CountQuantityLimitStrategy;

public record CountQuantityLimitDto(
        Boolean expenseCountLimitEnabled,
        CountQuantityLimitStrategy countQuantityLimitStrategy,
        int numberOfQuantityLimit
) {
}
