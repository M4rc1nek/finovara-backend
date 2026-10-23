package com.finovara.authservice.settings.security.operationauthorization.service.attempts;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.util.attempts.AttemptsHandler;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class AdditionalAuthorizationAttemptsHandler implements AttemptsHandler {

    private final Long userId;
    private final SecuritySettingsRepository securitySettingsRepository;

    private SecuritySettings lockedSecuritySettings;

    @Override
    public LocalDateTime getAttemptsExpiresAt() {
        return getLockedSecuritySettings().getAdditionalAuthorizationAttemptsExpiresAt();
    }

    @Override
    public int getCurrentAttempts() {
        return getLockedSecuritySettings().getAdditionalAuthorizationAttempts();
    }

    @Override
    public int incrementAttempts(int maxAttempts) {
        SecuritySettings securitySettings = getLockedSecuritySettings();
        if (securitySettings.getAdditionalAuthorizationAttempts() >= maxAttempts) {
            return 0;
        }
        securitySettings.setAdditionalAuthorizationAttempts(securitySettings.getAdditionalAuthorizationAttempts() + 1);
        return 1;
    }

    @Override
    public void resetAttempts(int value, LocalDateTime expiresAt) {
        SecuritySettings securitySettings = getLockedSecuritySettings();
        securitySettings.setAdditionalAuthorizationAttempts(value);
        securitySettings.setAdditionalAuthorizationAttemptsExpiresAt(expiresAt);
    }

    private SecuritySettings getLockedSecuritySettings() {
        if (lockedSecuritySettings == null) {
            lockedSecuritySettings = securitySettingsRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new RequestedEntityNotFoundException("Security settings not found"));
        }
        return lockedSecuritySettings;
    }
}