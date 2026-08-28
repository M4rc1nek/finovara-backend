package com.finovara.financeservice.settings.finances.recurring.dto;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.RecurringType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringOccurrenceDto(
        LocalDate date,
        RecurringType type,
        BigDecimal amount,
        ExpenseCategory expenseCategory,
        RevenueCategory revenueCategory,
        Long recurringSettingsId,
        Long piggyBankId
) {}