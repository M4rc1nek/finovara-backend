package com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.dto;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangesActivityDto(
        AccountChangesActivityType type,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
