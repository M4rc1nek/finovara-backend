package com.finovara.authservice.settings.security.twofactor.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.settings.security.twofactor.dto.AuthenticatorSetupResponse;
import com.finovara.authservice.settings.security.twofactor.dto.RecoveryCodesResponse;
import com.finovara.authservice.settings.security.twofactor.dto.TwoFactorCodeRequest;
import com.finovara.authservice.settings.security.twofactor.dto.TwoFactorStatusResponse;
import com.finovara.authservice.settings.security.twofactor.service.TwoFactorSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security-settings/two-factor")
@RequiredArgsConstructor
public class TwoFactorSettingsController {

    private final TwoFactorSettingsService twoFactorSettingsService;

    @GetMapping
    public ResponseEntity<TwoFactorStatusResponse> getTwoFactorStatus() {
        return ResponseEntity.ok(twoFactorSettingsService.getTwoFactorStatus(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/setup")
    public ResponseEntity<AuthenticatorSetupResponse> startAuthenticatorSetup() {
        return ResponseEntity.ok(twoFactorSettingsService.startAuthenticatorSetup(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<RecoveryCodesResponse> enableTwoFactor(@Valid @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(twoFactorSettingsService.enableTwoFactor(SecurityUtils.getCurrentUserId(), request.code()));
    }

    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<RecoveryCodesResponse> regenerateRecoveryCodes(@Valid @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(twoFactorSettingsService.regenerateRecoveryCodes(SecurityUtils.getCurrentUserId(), request.code()));
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disableTwoFactor(@Valid @RequestBody TwoFactorCodeRequest request) {
        twoFactorSettingsService.disableTwoFactor(SecurityUtils.getCurrentUserId(), request.code());
        return ResponseEntity.noContent().build();
    }
}
