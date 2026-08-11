package com.finovara.authservice.settings.security.operationauthorization.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeRequest;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeResponse;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationRequest;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationSettingsResponse;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationEmailVerificationService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<Void> saveAdditionalAuthorization(@Valid @RequestBody AdditionalAuthorizationRequest request, HttpServletRequest httpServletRequest) {
        additionalAuthorizationService.saveAdditionalAuthorization(SecurityUtils.getCurrentUserId(), request, httpServletRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<AdditionalAuthorizationSettingsResponse> getAdditionalAuthorizationSettings() {
        return ResponseEntity.ok(additionalAuthorizationService.getAdditionalAuthorizationSettings(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/visible")
    public ResponseEntity<Boolean> visibleAdditionalAuthorization() {
        return ResponseEntity.ok(additionalAuthorizationService.visibleAdditionalAuthorization(SecurityUtils.getCurrentUserId()));
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
    public ResponseEntity<AdditionalAuthorizationEmailCodeResponse> confirmAdditionalAuthorizationEmailCode(@RequestBody @Valid AdditionalAuthorizationEmailCodeRequest dto, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(additionalAuthorizationEmailVerificationService.confirmAdditionalAuthorizationCode(SecurityUtils.getCurrentUserId(), dto, httpServletRequest));
    }

}