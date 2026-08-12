package com.finovara.authservice.util.attempts.dto;

public record AttemptsContext(
        int maxAttempts,
        int attemptsExpirationMinutes,
        String exceededMessage
) {
}