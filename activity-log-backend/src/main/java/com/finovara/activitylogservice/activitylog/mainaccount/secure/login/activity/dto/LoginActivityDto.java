package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto;

import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;

import java.time.LocalDateTime;

public record LoginActivityDto(
        String type,
        LoginActivityStatus status,
        LocalDateTime createdAt,
        String browser,
        String ipAddress,
        String location
) {
}
