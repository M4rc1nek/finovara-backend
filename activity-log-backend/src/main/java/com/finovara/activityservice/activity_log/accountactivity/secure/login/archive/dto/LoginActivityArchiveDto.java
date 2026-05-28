package com.finovara.activityservice.activity_log.accountactivity.secure.login.archive.dto;

import com.finovara.contracts.model.activity.LoginActivityStatus;

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
