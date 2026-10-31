package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto;

import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record LoginActivityArchiveFilterDto(
       String type,
       LoginActivityStatus status,
       String browser,
       String ipAddress,
       String location,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate moveToArchiveDate,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
