package com.finovara.authservice.settings.security.operationauthorization.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationDto;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationRequest;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
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

    @PostMapping
    public ResponseEntity<ConfirmAuthorizationCodeDto> saveAdditionalAuthorization(@Valid @RequestBody AdditionalAuthorizationRequest request) {
        return ResponseEntity.ok(additionalAuthorizationService.saveAdditionalAuthorization(SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<AdditionalAuthorizationDto> getAdditionalAuthorizationSettings() {
        return ResponseEntity.ok(additionalAuthorizationService.getAdditionalAuthorizationSettings(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/regenerate")
    public ResponseEntity<ConfirmAuthorizationCodeDto> regenerateCode(@RequestBody ConfirmPasswordDto confirmPasswordDto) {
        return ResponseEntity.ok(additionalAuthorizationService.regenerateCode(SecurityUtils.getCurrentUserId(), confirmPasswordDto));
    }

}