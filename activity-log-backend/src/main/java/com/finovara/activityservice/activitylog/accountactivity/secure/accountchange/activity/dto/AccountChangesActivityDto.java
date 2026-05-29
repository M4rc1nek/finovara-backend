package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.dto;

import com.finovara.contracts.model.activity.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangesActivityDto(
        AccountChangesActivityType type,
        LocalDateTime createdAt,
        String browser,
        String ipAddress,
        String location
) {
}
