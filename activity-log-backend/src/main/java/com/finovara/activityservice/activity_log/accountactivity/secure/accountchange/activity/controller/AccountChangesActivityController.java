package com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.controller;

import com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.activityservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
