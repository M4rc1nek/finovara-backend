package com.finovara.finovarabackend.accountactivity.accountchanges.dto;

import com.finovara.finovarabackend.accountactivity.accountchanges.model.UserActivityAccountChangesType;

import java.time.LocalDateTime;

public record UserActivityAccountChangesDto(
        UserActivityAccountChangesType type,
        LocalDateTime date,
        String browser,
        String ipAddress,
        String location
) {
}
