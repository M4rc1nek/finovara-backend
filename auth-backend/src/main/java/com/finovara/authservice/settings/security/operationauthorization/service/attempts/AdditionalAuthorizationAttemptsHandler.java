package com.finovara.authservice.settings.security.operationauthorization.service.attempts;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.util.attempts.AttemptsHandler;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class AdditionalAuthorizationAttemptsHandler implements AttemptsHandler {

    private final Long userId;
    private final SecuritySettings settings;
    private final SecuritySettingsRepository securitySettingsRepository;


    @Override
    public LocalDateTime getAttemptsExpiresAt() {
        return settings.getAdditionalAuthorizationAttemptsExpiresAt();
    }

    @Override
    public int getCurrentAttempts() {
        return securitySettingsRepository.getAdditionalAuthorizationAttemptsByUserId(userId);
    }

    @Override
    public int incrementAttempts(int maxAttempts) {
        return securitySettingsRepository.incrementAdditionalAuthorizationAttempts(userId, maxAttempts);
    }

    @Override
    public void resetAttempts(int value, LocalDateTime expiresAt) {
        settings.setAdditionalAuthorizationAttempts(value);
        settings.setAdditionalAuthorizationAttemptsExpiresAt(expiresAt);
        securitySettingsRepository.save(settings);
    }
}