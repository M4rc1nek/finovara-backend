package com.finovara.financeservice.internal.financial.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategorySumDto(ExpenseCategory category, BigDecimal amount) {
}
