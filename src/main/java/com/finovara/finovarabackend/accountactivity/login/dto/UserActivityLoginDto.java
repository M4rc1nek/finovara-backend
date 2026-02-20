package com.finovara.finovarabackend.accountactivity.login.dto;

import com.finovara.finovarabackend.accountactivity.login.model.UserActivityLoginStatus;

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
