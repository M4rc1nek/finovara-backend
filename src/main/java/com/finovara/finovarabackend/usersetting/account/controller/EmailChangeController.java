package com.finovara.finovarabackend.usersetting.account.controller;

import com.finovara.finovarabackend.usersetting.account.dto.ChangeEmailDto;
import com.finovara.finovarabackend.usersetting.account.service.ChangeEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-change")
@RequiredArgsConstructor
public class EmailChangeController {

    private final ChangeEmailService changeEmailService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> sendEmail(@PathVariable Long userId, @RequestBody @Valid ChangeEmailDto changeEmailDto) {
        changeEmailService.emailSend(userId, changeEmailDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/verify-code")
    public ResponseEntity<Void> verifyCode(@PathVariable Long userId, @RequestBody @Valid ChangeEmailDto changeEmailDto) {
        changeEmailService.verifyCode(userId, changeEmailDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/change-email")
    public ResponseEntity<Void> changeEmailAddress(@PathVariable Long userId, @RequestBody @Valid ChangeEmailDto changeEmailDto) {
        changeEmailService.changeEmailAddressWithCode(userId, changeEmailDto);
        return ResponseEntity.noContent().build();
    }
}