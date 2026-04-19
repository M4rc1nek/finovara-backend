package com.finovara.finovarabackend.usersetting.account.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.account.dto.ChangeEmailDto;
import com.finovara.finovarabackend.usersetting.account.service.emailpolicy.ChangeEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-change")
@RequiredArgsConstructor
public class EmailChangeController {

    private final ChangeEmailService changeEmailService;

    @PostMapping
    public ResponseEntity<Void> requestEmailChange(@RequestBody @Valid ChangeEmailDto dto) {
        changeEmailService.requestEmailChange(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/confirm")
    public ResponseEntity<Void> confirmEmailWithCode(@RequestBody @Valid ChangeEmailDto dto) {
        changeEmailService.changeEmailAddressWithCode(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}