package com.finovara.activitylogservice.activitylog.mainaccount.limit.dto;

import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;
import com.finovara.contracts.util.PeriodType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitActivityFilterDto(
       LimitActivityType limitActivityType,
       PeriodType periodType,
       BigDecimal amount,
       BigDecimal previousAmount,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
