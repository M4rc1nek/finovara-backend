package com.finovara.finovarabackend.accountactivity.accountchange.archive.dto;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;

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
