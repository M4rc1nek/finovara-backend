package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AccountChangeArchiveFilterDto(
       AccountChangeActivityType type,
       String browser,
       String ipAddress,
       String location,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate moveToArchiveDate,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
