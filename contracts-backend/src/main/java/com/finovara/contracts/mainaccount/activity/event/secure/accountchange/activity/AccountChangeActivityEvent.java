package com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;

import java.time.LocalDateTime;

public record AccountChangeActivityEvent(
        Long userId,
        AccountChangeActivityType type,
        String browser,
        String ipAddress,
        String location,
        LocalDateTime occurredAt
) {
}