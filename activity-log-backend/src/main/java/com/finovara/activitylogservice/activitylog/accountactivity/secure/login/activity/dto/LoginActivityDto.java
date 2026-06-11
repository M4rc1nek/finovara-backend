package com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.dto;

import com.finovara.contracts.model.activity.LoginActivityStatus;

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
