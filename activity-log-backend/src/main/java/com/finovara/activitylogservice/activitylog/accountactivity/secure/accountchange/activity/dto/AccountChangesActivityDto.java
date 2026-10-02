package com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.dto;

import com.finovara.contracts.mainaccount.activity.model.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangesActivityDto(
        AccountChangesActivityType type,
        LocalDateTime createdAt,
        String browser,
        String ipAddress,
        String location
) {
}
