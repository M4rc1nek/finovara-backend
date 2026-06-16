package com.finovara.authservice.settings.account.dto;

public record AttemptsDto(
        int used,
        int max,
        int remaining
) {
}
