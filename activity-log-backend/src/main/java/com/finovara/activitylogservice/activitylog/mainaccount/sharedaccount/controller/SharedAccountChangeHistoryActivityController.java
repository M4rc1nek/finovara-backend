package com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.controller;

import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.dto.SharedAccountChangeHistoryActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.service.SharedAccountChangeHistoryActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/shared-account")
@RequiredArgsConstructor
public class SharedAccountChangeHistoryActivityController {

    private final SharedAccountChangeHistoryActivityService sharedAccountChangeHistoryActivityService;

    @GetMapping
    public ResponseEntity<List<SharedAccountChangeHistoryActivityDto>> getSharedAccountActivity(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(sharedAccountChangeHistoryActivityService.getSharedAccountActivity(SecurityUtils.getCurrentUserId(), sort));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        sharedAccountChangeHistoryActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}