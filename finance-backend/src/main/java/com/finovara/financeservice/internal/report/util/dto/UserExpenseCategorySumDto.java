package com.finovara.financeservice.internal.report.util.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record UserExpenseCategorySumDto(Long userId, ExpenseCategory category, BigDecimal amount) {
}
