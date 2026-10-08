package com.finovara.activitylogservice.activitylog.mainaccountactivity.secure.accountchange.archive.dto;

import com.finovara.contracts.mainaccount.activity.model.AccountChangesActivityType;

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
