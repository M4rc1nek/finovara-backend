package com.finovara.activitylogservice.activitylog.mainaccount.expense.dto;

import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.util.model.ExpenseCategory;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseActivityFilterDto(
       ExpenseActivityType type,
       BigDecimal amount,
       BigDecimal previousAmount,
       ExpenseCategory category,
       ExpenseCategory previousCategory,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
