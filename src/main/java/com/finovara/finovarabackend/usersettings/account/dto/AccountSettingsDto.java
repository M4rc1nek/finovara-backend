package com.finovara.finovarabackend.usersettings.account.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AccountSettingsDto(
        @Size(min = 3, max = 13)
        String username,

        String email,
        LocalDateTime createdAt,
        String profileImageUrl

) {
}