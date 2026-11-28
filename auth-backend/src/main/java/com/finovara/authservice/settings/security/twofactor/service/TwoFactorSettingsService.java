package com.finovara.authservice.twofactor.service;

import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.twofactor.dto.AuthenticatorSetupResponse;
import com.finovara.authservice.twofactor.dto.RecoveryCodesResponse;
import com.finovara.authservice.twofactor.dto.TwoFactorStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorSettingsService {

    private final TwoFactorSettingsOperations settingsOperations;

    public TwoFactorStatusResponse getTwoFactorStatus(Long userId) {
        return settingsOperations.getTwoFactorStatus(userId);
    }

    public AuthenticatorSetupResponse startAuthenticatorSetup(Long userId) {
        return settingsOperations.startAuthenticatorSetup(userId);
    }

    public RecoveryCodesResponse enableTwoFactor(Long userId, String code) {
        return settingsOperations.enableTwoFactor(userId, code)
                .orElseThrow(this::invalidAuthenticatorCode);
    }

    public RecoveryCodesResponse regenerateRecoveryCodes(Long userId, String code) {
        return settingsOperations.regenerateRecoveryCodes(userId, code)
                .orElseThrow(this::invalidAuthenticatorCode);
    }

    public void disableTwoFactor(Long userId, String code) {
        if (!settingsOperations.disableTwoFactor(userId, code)) {
            throw invalidAuthenticatorCode();
        }
    }

    public boolean verifyLoginChallengeCode(Long userId, String code) {
        return settingsOperations.verifyLoginChallengeCode(userId, code);
    }

    private InvalidCredentialsException invalidAuthenticatorCode() {
        return new InvalidCredentialsException("Invalid authenticator code");
    }
}
