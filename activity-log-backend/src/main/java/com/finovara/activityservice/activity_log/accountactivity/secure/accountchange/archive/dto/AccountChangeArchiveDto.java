package com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.archive.dto;

import com.finovara.activityservice.contracts.model.activity.AccountChangesActivityType;

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
