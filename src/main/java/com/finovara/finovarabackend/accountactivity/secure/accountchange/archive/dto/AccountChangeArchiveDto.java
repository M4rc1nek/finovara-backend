package com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.dto;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;

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
