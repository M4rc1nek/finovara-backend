package com.finovara.corebackend.usersetting.account.controller;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.corebackend.usersetting.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.corebackend.usersetting.account.service.AccountService;
import com.finovara.corebackend.usersetting.account.service.passwordpolicy.change.ChangePasswordService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
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

    @PutMapping("/{userId}/username")
    public ResponseEntity<AccountSettingsDto> updateUsername(@RequestBody @Valid AccountSettingsDto accountSettingsDto, @PathVariable Long userId, HttpServletRequest request) {
        return ResponseEntity.ok(accountService.updateUsername(accountSettingsDto, userId, request));
    }

    @GetMapping
    public ResponseEntity<AccountSettingsDto> getAccountSettings() {
        return ResponseEntity.ok(accountService.getAccountSettings(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAccount(@RequestBody ConfirmPasswordDto confirmPasswordDto, @PathVariable Long userId) {
        accountService.deleteAccount(confirmPasswordDto, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void> changeUserPassword(@RequestBody @Valid ChangePasswordDto changePasswordDto, HttpServletRequest request) {
        changePasswordService.changePassword(SecurityUtils.getCurrentUserId(), changePasswordDto, request);
        return ResponseEntity.noContent().build();
    }

}
