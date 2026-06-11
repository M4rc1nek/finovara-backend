package com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.archive.dto;

import com.finovara.contracts.model.activity.AccountChangesActivityType;

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
