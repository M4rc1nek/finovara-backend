package com.finovara.activitylogservice.activitylog.mainaccount.piggybank.dto;

import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.util.model.PiggyBankGoalType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PiggyBankActivityFilterDto(
       PiggyBankActivityType activityType,
       String piggyBankName,
       PiggyBankGoalType goalType,
       BigDecimal goalAmount,
       BigDecimal amountPaid,
       BigDecimal amountPaidOut,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
