package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AccountChangeActivityFilterDto(
       AccountChangeActivityType type,
       String browser,
       String ipAddress,
       String location,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
