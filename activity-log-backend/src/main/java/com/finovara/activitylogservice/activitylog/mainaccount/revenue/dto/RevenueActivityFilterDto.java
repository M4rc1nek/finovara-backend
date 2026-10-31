package com.finovara.activitylogservice.activitylog.mainaccount.revenue.dto;

import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.util.model.RevenueCategory;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueActivityFilterDto(
       RevenueActivityType type,
       BigDecimal amount,
       BigDecimal previousAmount,
       RevenueCategory category,
       RevenueCategory previousCategory,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
