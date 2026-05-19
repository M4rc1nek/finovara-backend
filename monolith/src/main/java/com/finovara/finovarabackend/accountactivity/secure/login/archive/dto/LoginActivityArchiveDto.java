package com.finovara.finovarabackend.accountactivity.secure.login.archive.dto;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;

import java.time.LocalDateTime;

public record LoginActivityArchiveDto(
        String type,
        LoginActivityStatus status,
        LocalDateTime moveToArchiveDate,
        LocalDateTime activityLoginDate,
        String browser,
        String ipAddress,
        String location
) {
}
