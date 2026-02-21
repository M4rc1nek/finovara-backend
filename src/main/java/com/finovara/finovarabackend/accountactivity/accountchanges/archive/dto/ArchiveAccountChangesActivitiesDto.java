package com.finovara.finovarabackend.accountactivity.accountchanges.archive.dto;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChangesType;

import java.time.LocalDateTime;

public record ArchiveAccountChangesActivitiesDto(
        UserActivityAccountChangesType type,
        LocalDateTime moveToArchiveDate,
        LocalDateTime activityAccountChangesDate,
        String browser,
        String ipAddress,
        String location
) {
}
