package com.finovara.finovarabackend.accountactivity.secure.accountchange.activities.dto;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activities.model.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangesActivityDto(
        AccountChangesActivityType type,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
