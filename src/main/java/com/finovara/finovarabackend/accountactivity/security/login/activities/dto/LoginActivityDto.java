package com.finovara.finovarabackend.accountactivity.security.login.activities.dto;

import com.finovara.finovarabackend.accountactivity.security.login.activities.model.LoginActivityStatus;

import java.time.LocalDateTime;

public record LoginActivityDto(
        String type,
        LoginActivityStatus status,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
