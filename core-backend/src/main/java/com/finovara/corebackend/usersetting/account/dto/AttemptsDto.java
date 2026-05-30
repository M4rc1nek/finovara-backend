package com.finovara.corebackend.usersetting.account.dto;

public record AttemptsDto(
        int used,
        int max,
        int remaining
) {
}
