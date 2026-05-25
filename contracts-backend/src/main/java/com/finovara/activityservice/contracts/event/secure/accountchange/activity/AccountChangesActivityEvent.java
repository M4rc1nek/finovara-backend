package com.finovara.activityservice.contracts.event.secure.accountchange.activity;

import com.finovara.activityservice.contracts.model.activity.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangesActivityEvent(
        Long userId,
        AccountChangesActivityType type,
        String browser,
        String ipAddress,
        String location,
        LocalDateTime occurredAt
) {
}