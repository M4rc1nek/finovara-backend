package com.finovara.authservice.settings.factory;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.security.SecuritySettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsFactory {

    public AccountSettings createDefaultAccountSettings() {
        return AccountSettings.builder()
                .emailChangeCode(null)
                .pendingEmail(null)
                .resetPasswordCode(null)
                .resetPasswordCodeExpiresAt(null)
                .attemptsEmailExpiresAt(null)
                .attemptsPasswordExpiresAt(null)
                .build();
    }

    public SecuritySettings createDefaultSecuritySettings() {
        return SecuritySettings.builder()
                .additionalAuthorizationEnabled(false)
                .additionalAuthorizationVisible(false)
                .additionalAuthorizationAttempts(0)
                .twoFactorEnabled(false)
                .twoFactorSecret(null)
                .twoFactorPendingSecret(null)
                .twoFactorPendingExpiresAt(null)
                .twoFactorBackupCodes(null)
                .twoFactorFailedAttempts(0)
                .twoFactorLockedUntil(null)
                .twoFactorLastUsedStep(null)
                .build();
    }

}
