package com.finovara.authservice.settings.security.operationauthorization.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.settings.security.operationauthorization.dto.*;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationEmailVerificationService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security-settings/additional-authorization")
@RequiredArgsConstructor
public class AdditionalAuthorizationController {

    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationEmailVerificationService additionalAuthorizationEmailVerificationService;

    @PostMapping
    public ResponseEntity<Void> saveAdditionalAuthorization(@Valid @RequestBody AdditionalAuthorizationRequest request) {
        additionalAuthorizationService.saveAdditionalAuthorization(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<AdditionalAuthorizationSettingsResponse> getAdditionalAuthorizationSettings() {
        return ResponseEntity.ok(additionalAuthorizationService.getAdditionalAuthorizationSettings(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/regenerate")
    public ResponseEntity<Void> regenerateCode(@RequestBody ConfirmPasswordDto confirmPasswordDto) {
        additionalAuthorizationService.regenerateCode(SecurityUtils.getCurrentUserId(), confirmPasswordDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request-email-code")
    public ResponseEntity<Void> requestAdditionalAuthorizationEmailCode() {
        additionalAuthorizationEmailVerificationService.requestAdditionalAuthorizationEmail(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/confirm-email-code")
    public ResponseEntity<AdditionalAuthorizationEmailCodeResponse> confirmAdditionalAuthorizationEmailCode(@RequestBody @Valid AdditionalAuthorizationEmailCodeRequest dto) {
        return ResponseEntity.ok(additionalAuthorizationEmailVerificationService.confirmAdditionalAuthorizationCode(SecurityUtils.getCurrentUserId(), dto));
    }


}