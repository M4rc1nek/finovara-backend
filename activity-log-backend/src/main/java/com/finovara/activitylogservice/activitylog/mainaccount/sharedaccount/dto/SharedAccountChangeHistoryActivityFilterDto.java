package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto;

import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SharedAccountChangeHistoryActivityFilterDto(
       SharedAccountChangeHistoryActivityType type,
       BigDecimal refundedBalance,
       String coFounderUsername,
       String coFounderEmail,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
