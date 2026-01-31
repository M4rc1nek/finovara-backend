package com.finovara.finovarabackend.usersettings.account.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersettings.account.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.usersettings.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account-settings")
public class AccountSettingsController {
    private final AccountService accountService;

    @PutMapping("/{userId}/username")
    public ResponseEntity<AccountSettingsDto> updateUsername(@RequestBody AccountSettingsDto accountSettingsDto, @PathVariable Long userId) {
        return ResponseEntity.ok(accountService.updateUsername(accountSettingsDto, userId));
    }

    @GetMapping
    public ResponseEntity<AccountSettingsDto> getAccountSettings() {
        return ResponseEntity.ok(accountService.getAccountSettings(SecurityUtils.getCurrentUserEmail()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAccount(@RequestBody ConfirmPasswordDto confirmPasswordDto, @PathVariable Long userId) {
        accountService.deleteAccount(confirmPasswordDto,userId);
        return ResponseEntity.noContent().build();
    }

}
