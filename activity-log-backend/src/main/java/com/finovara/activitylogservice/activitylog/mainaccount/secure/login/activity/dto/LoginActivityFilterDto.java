package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto;

import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record LoginActivityFilterDto(
       LoginActivityStatus status,
       String browser,
       String ipAddress,
       String location,
       @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate createdAt
) {}
