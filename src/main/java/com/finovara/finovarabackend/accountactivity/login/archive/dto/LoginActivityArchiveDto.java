package com.finovara.finovarabackend.accountactivity.login.archive.dto;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivityStatus;

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
