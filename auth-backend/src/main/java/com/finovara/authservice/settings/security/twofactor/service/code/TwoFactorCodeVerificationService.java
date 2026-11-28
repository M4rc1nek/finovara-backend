package com.finovara.authservice.settings.security.twofactor.service.code;

import com.finovara.authservice.settings.security.SecuritySettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TwoFactorCodeVerificationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final AuthenticatorCredentialService authenticatorCredentialService;

    public int getRemainingRecoveryCodeCount(SecuritySettings settings) {
        return loadRecoveryCodeHashes(settings).size();
    }

    public boolean verifyAuthenticatorCode(SecuritySettings settings, String secret, String code) {
        if (isLocked(settings)) {
            return false;
        }

        Long matchedStep = authenticatorCredentialService.findCodeTimeStep(
                secret, code, System.currentTimeMillis() / 1000);
        Long lastUsedStep = settings.getTwoFactorLastUsedStep();
        if (matchedStep == null || (lastUsedStep != null && matchedStep <= lastUsedStep)) {
            return false;
        }

        settings.setTwoFactorLastUsedStep(matchedStep);
        resetFailedAttempts(settings);
        return true;
    }

    public boolean verifyAndConsumeRecoveryCode(SecuritySettings settings, String code) {
        if (code == null) {
            return false;
        }

        List<String> hashes = loadRecoveryCodeHashes(settings);
        String matchedHash = hashes.stream()
                .filter(hash -> authenticatorCredentialService.matchesRecoveryCode(code, hash))
                .findFirst()
                .orElse(null);
        if (matchedHash == null) {
            registerFailedAttempt(settings);
            return false;
        }

        hashes.remove(matchedHash);
        settings.setTwoFactorBackupCodes(String.join("\n", hashes));
        resetFailedAttempts(settings);
        return true;
    }

    public void registerFailedAttempt(SecuritySettings settings) {
        int failedAttempts = settings.getTwoFactorFailedAttempts() + 1;
        settings.setTwoFactorFailedAttempts(failedAttempts);
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            settings.setTwoFactorLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
    }

    public boolean isLocked(SecuritySettings settings) {
        LocalDateTime lockedUntil = settings.getTwoFactorLockedUntil();
        if (lockedUntil == null) {
            return false;
        }
        if (lockedUntil.isAfter(LocalDateTime.now())) {
            return true;
        }
        resetFailedAttempts(settings);
        return false;
    }

    public void resetFailedAttempts(SecuritySettings settings) {
        settings.setTwoFactorFailedAttempts(0);
        settings.setTwoFactorLockedUntil(null);
    }

    public String hashRecoveryCodes(List<String> codes) {
        List<String> hashes = codes.stream()
                .map(authenticatorCredentialService::hashRecoveryCode)
                .toList();
        return String.join("\n", hashes);
    }

    private List<String> loadRecoveryCodeHashes(SecuritySettings settings) {
        String storedHashes = settings.getTwoFactorBackupCodes();
        if (storedHashes == null || storedHashes.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(storedHashes.split("\n")));
    }
}
