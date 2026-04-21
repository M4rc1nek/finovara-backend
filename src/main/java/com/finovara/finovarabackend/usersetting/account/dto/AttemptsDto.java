package com.finovara.finovarabackend.usersetting.account.dto;

public record AttemptsDto(
        int used,
        int max,
        int remaining
) {
}
