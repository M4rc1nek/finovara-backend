package com.finovara.corebackend.exception;

import com.finovara.corebackend.usersetting.account.dto.AttemptsDto;

public record AttemptsErrorResponseDto(
        int status,
        String error,
        String message,
        String path,
        AttemptsDto attempts,
        long timestamp
) {

    public AttemptsErrorResponseDto(int status, String error, String message, String path, AttemptsDto attempts) {
        this(status, error, message, path, attempts, System.currentTimeMillis());
    }
}

