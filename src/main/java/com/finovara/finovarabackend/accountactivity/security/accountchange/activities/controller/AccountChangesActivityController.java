package com.finovara.finovarabackend.accountactivity.security.accountchange.activities.controller;

import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.dto.AccountChangesActivityDto;
import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/account-changes")
@RequiredArgsConstructor
public class AccountChangesActivityController {

    private final AccountChangesActivityService accountChangesActivityService;

    @GetMapping
    public ResponseEntity<List<AccountChangesActivityDto>> getAccountChangesActivity() {
        return ResponseEntity.ok(accountChangesActivityService.getAccountChangesActivity(SecurityUtils.getCurrentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<Void> confirmPasswordToAccountChangesActivity(@RequestBody ConfirmPasswordDto confirmPasswordDto) {
        accountChangesActivityService.confirmPasswordToAccountChangesActivity(SecurityUtils.getCurrentUserEmail(), confirmPasswordDto);
        return ResponseEntity.noContent().build();
    }
}
