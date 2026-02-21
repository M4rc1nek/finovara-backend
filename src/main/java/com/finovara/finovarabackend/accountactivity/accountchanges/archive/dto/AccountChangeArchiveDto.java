package com.finovara.finovarabackend.accountactivity.accountchanges.archive.dto;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangeArchiveDto(
        AccountChangesActivityType type,
        LocalDateTime moveToArchiveDate,
        LocalDateTime activityAccountChangesDate,
        String browser,
        String ipAddress,
        String location
) {
}
