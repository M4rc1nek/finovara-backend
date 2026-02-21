package com.finovara.finovarabackend.accountactivity.accountchanges.activities.dto;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChangesType;

import java.time.LocalDateTime;

public record UserActivityAccountChangesDto(
        UserActivityAccountChangesType type,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
