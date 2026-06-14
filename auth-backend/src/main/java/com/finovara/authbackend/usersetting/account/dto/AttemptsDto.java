package com.finovara.authbackend.usersetting.account.dto;

public record AttemptsDto(
        int used,
        int max,
        int remaining
) {
}
