package com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.controller;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
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
        return ResponseEntity.ok(accountChangesActivityService.getAccountChangesActivity(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        accountChangesActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

}
