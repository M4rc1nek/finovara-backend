package com.finovara.authservice.settings.account.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.settings.account.dto.AccountSettingsDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.authservice.settings.account.service.AccountService;
import com.finovara.authservice.settings.account.service.passwordpolicy.change.ChangePasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account-settings")
public class AccountSettingsController {
    private final AccountService accountService;
    private final ChangePasswordService changePasswordService;

    @PutMapping("/username")
    public ResponseEntity<AccountSettingsDto> updateUsername(@RequestBody @Valid AccountSettingsDto accountSettingsDto, HttpServletRequest request) {
        return ResponseEntity.ok(accountService.updateUsername(accountSettingsDto, SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<AccountSettingsDto> getAccountSettings() {
        return ResponseEntity.ok(accountService.getAccountSettings(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping
    public ResponseEntity<Void> changeUserPassword(@RequestBody @Valid ChangePasswordDto changePasswordDto, HttpServletRequest request) {
        changePasswordService.changePassword(SecurityUtils.getCurrentUserId(), changePasswordDto, request);
        return ResponseEntity.noContent().build();
    }

}
