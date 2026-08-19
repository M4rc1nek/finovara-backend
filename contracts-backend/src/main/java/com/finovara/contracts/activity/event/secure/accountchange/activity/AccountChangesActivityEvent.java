package com.finovara.contracts.activity.event.secure.accountchange.activity;

import com.finovara.contracts.model.activity.AccountChangesActivityType;

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