package com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity;

import com.finovara.contracts.mainaccount.activity.model.AccountChangesActivityType;

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