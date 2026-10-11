package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;

import java.time.LocalDateTime;

public record AccountChangeArchiveDto(
        AccountChangeActivityType type,
        LocalDateTime moveToArchiveDate,
        LocalDateTime createdAt,
        String browser,
        String ipAddress,
        String location
) {
}
