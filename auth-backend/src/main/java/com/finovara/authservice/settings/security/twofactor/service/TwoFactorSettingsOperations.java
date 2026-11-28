package com.finovara.authservice.twofactor.service;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.twofactor.service.code.AuthenticatorCredentialService;
import com.finovara.authservice.twofactor.service.code.TwoFactorCodeVerificationService;
import com.finovara.authservice.twofactor.dto.AuthenticatorSetupResponse;
import com.finovara.authservice.twofactor.dto.RecoveryCodesResponse;
import com.finovara.authservice.twofactor.dto.TwoFactorStatusResponse;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TwoFactorSettingsOperations {

    private final UserRepository userRepository;
    private final AuthenticatorCredentialService authenticatorCredentialService;
    private final TwoFactorCodeVerificationService codeVerificationService;

    @Transactional(readOnly = true)
    public TwoFactorStatusResponse getTwoFactorStatus(Long userId) {
        SecuritySettings settings = loadSecuritySettings(userId);
        return new TwoFactorStatusResponse(
                settings.isTwoFactorEnabled(),
                codeVerificationService.getRemainingRecoveryCodeCount(settings));
    }

    @Transactional
    public AuthenticatorSetupResponse startAuthenticatorSetup(Long userId) {
        User user = loadUserWithSecuritySettingsForUpdate(userId);
        SecuritySettings settings = user.getSecuritySettings();
        if (settings.isTwoFactorEnabled()) {
            throw new EntityAlreadyExistsException("Two-factor authentication is already enabled");
        }

        String secret = authenticatorCredentialService.generateAuthenticatorSecret();
        settings.setTwoFactorPendingSecret(authenticatorCredentialService.encryptAuthenticatorSecret(secret));
        settings.setTwoFactorPendingExpiresAt(LocalDateTime.now().plusMinutes(10));
        return new AuthenticatorSetupResponse(secret, authenticatorCredentialService.createAuthenticatorSetupUri(secret, user.getEmail()));
    }

    @Transactional
    public Optional<RecoveryCodesResponse> enableTwoFactor(Long userId, String code) {
        SecuritySettings settings = loadUserWithSecuritySettingsForUpdate(userId).getSecuritySettings();
        String pendingSecret = settings.getTwoFactorPendingSecret();
        if (isSetupExpiredOrMissing(settings, pendingSecret)) {
            clearPendingSetup(settings);
            return Optional.empty();
        }

        String secret = authenticatorCredentialService.decryptAuthenticatorSecret(pendingSecret);
        if (!codeVerificationService.verifyAuthenticatorCode(settings, secret, code)) {
            codeVerificationService.registerFailedAttempt(settings);
            return Optional.empty();
        }

        List<String> recoveryCodes = authenticatorCredentialService.generateRecoveryCodes();
        settings.setTwoFactorSecret(authenticatorCredentialService.encryptAuthenticatorSecret(secret));
        clearPendingSetup(settings);
        settings.setTwoFactorBackupCodes(codeVerificationService.hashRecoveryCodes(recoveryCodes));
        settings.setTwoFactorEnabled(true);
        codeVerificationService.resetFailedAttempts(settings);
        return Optional.of(new RecoveryCodesResponse(recoveryCodes));
    }

    @Transactional
    public Optional<RecoveryCodesResponse> regenerateRecoveryCodes(Long userId, String code) {
        SecuritySettings settings = loadUserWithSecuritySettingsForUpdate(userId).getSecuritySettings();
        if (!verifyAccountCode(settings, code)) {
            return Optional.empty();
        }

        List<String> recoveryCodes = authenticatorCredentialService.generateRecoveryCodes();
        settings.setTwoFactorBackupCodes(codeVerificationService.hashRecoveryCodes(recoveryCodes));
        codeVerificationService.resetFailedAttempts(settings);
        return Optional.of(new RecoveryCodesResponse(recoveryCodes));
    }

    @Transactional
    public boolean disableTwoFactor(Long userId, String code) {
        SecuritySettings settings = loadUserWithSecuritySettingsForUpdate(userId).getSecuritySettings();
        if (!verifyAccountCode(settings, code)) {
            return false;
        }

        settings.setTwoFactorEnabled(false);
        settings.setTwoFactorSecret(null);
        clearPendingSetup(settings);
        settings.setTwoFactorBackupCodes(null);
        settings.setTwoFactorLastUsedStep(null);
        codeVerificationService.resetFailedAttempts(settings);
        return true;
    }

    @Transactional
    public boolean verifyLoginChallengeCode(Long userId, String code) {
        SecuritySettings settings = loadUserWithSecuritySettingsForUpdate(userId).getSecuritySettings();
        if (!settings.isTwoFactorEnabled() || codeVerificationService.isLocked(settings)) {
            return false;
        }

        String secret = authenticatorCredentialService.decryptAuthenticatorSecret(settings.getTwoFactorSecret());
        if (codeVerificationService.verifyAuthenticatorCode(settings, secret, code)) {
            return true;
        }
        return codeVerificationService.verifyAndConsumeRecoveryCode(settings, code);
    }

    private boolean isSetupExpiredOrMissing(SecuritySettings settings, String pendingSecret) {
        return pendingSecret == null || settings.isTwoFactorEnabled() || settings.getTwoFactorPendingExpiresAt() == null || settings.getTwoFactorPendingExpiresAt().isBefore(LocalDateTime.now());
    }

    private boolean verifyAccountCode(SecuritySettings settings, String code) {
        if (!settings.isTwoFactorEnabled() || codeVerificationService.isLocked(settings)) {
            return false;
        }

        String secret = authenticatorCredentialService.decryptAuthenticatorSecret(settings.getTwoFactorSecret());
        return codeVerificationService.verifyAuthenticatorCode(settings, secret, code)|| codeVerificationService.verifyAndConsumeRecoveryCode(settings, code);
    }

    private void clearPendingSetup(SecuritySettings settings) {
        settings.setTwoFactorPendingSecret(null);
        settings.setTwoFactorPendingExpiresAt(null);
    }

    private SecuritySettings loadSecuritySettings(Long userId) {
        return userRepository.findWithSecuritySettingsById(userId)
                .map(User::getSecuritySettings)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }

    private User loadUserWithSecuritySettingsForUpdate(Long userId) {
        return userRepository.findWithSecuritySettingsByIdForUpdate(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
    }
}
