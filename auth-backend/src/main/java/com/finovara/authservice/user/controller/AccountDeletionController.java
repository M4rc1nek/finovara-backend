package com.finovara.authservice.user.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.sharedaccount.service.deletion.SharedAccountDeletionService;
import com.finovara.authservice.user.service.AccountDeletionService;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/account-management")
@RestController
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;
    private final SharedAccountDeletionService sharedAccountDeletionService;

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@RequestBody @Valid ConfirmPasswordDto confirmPasswordDto) {
        accountDeletionService.deleteAccount(confirmPasswordDto, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/membership")
    public ResponseEntity<Void> leaveTheSharedAccount(@RequestBody @Valid ConfirmPasswordDto confirmPasswordDto) {
        sharedAccountDeletionService.leaveSharedAccount(SecurityUtils.getCurrentUserId(), confirmPasswordDto);
        return ResponseEntity.noContent().build();
    }
}