package com.finovara.authservice.user.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.user.dto.ConfirmPasswordDto;
import com.finovara.authservice.user.service.AccountDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/users")
@RestController
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@RequestBody @Valid ConfirmPasswordDto confirmPasswordDto) {
        accountDeletionService.deleteAccount(confirmPasswordDto, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}