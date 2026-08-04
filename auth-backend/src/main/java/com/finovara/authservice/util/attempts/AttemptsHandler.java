package com.finovara.authservice.util.attempts;

import java.time.LocalDateTime;

public interface AttemptsHandler {

    LocalDateTime getAttemptsExpiresAt();

    int getCurrentAttempts();

    int incrementAttempts(int maxAttempts);

    void resetAttempts(int value, LocalDateTime expiresAt);
}