package com.finovara.finovarabackend.usersetting.account.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersetting.account.service.AccountService;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ChangePasswordService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
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
        return ResponseEntity.ok(accountService.getAccountSettings(SecurityUtils.getCurrentUserEmail()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAccount(@RequestBody ConfirmPasswordDto confirmPasswordDto, @PathVariable Long userId) {
        accountService.deleteAccount(confirmPasswordDto, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void> changeUserPassword(@RequestBody @Valid PasswordRequestDto passwordRequestDto, HttpServletRequest request) {
        changePasswordService.changePassword(SecurityUtils.getCurrentUserEmail(), passwordRequestDto, request);
        return ResponseEntity.noContent().build();
    }

}
