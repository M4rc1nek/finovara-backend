package com.finovara.finovarabackend.accountactivity.secure.login.activity.dto;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;

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
