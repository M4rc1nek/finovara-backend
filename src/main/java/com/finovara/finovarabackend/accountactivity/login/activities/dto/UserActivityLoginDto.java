package com.finovara.finovarabackend.accountactivity.login.activities.dto;

import com.finovara.finovarabackend.accountactivity.login.activities.model.UserActivityLoginStatus;

import java.time.LocalDateTime;

public record UserActivityLoginDto(
        String type,
        UserActivityLoginStatus status,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
