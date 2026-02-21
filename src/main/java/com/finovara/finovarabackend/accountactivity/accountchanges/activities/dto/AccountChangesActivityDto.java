package com.finovara.finovarabackend.accountactivity.accountchanges.activities.dto;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivityType;

import java.time.LocalDateTime;

public record AccountChangesActivityDto(
        AccountChangesActivityType type,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
