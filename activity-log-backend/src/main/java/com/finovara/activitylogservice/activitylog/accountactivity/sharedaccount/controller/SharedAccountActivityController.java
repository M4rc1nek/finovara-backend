package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.controller;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto.SharedAccountActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.service.SharedAccountActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/shared-account")
@RequiredArgsConstructor
public class SharedAccountActivityController {

    private final SharedAccountActivityService sharedAccountActivityService;

    @GetMapping
    public ResponseEntity<List<SharedAccountActivityDto>> getSharedAccountActivity(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(sharedAccountActivityService.getSharedAccountActivity(SecurityUtils.getCurrentUserId(), sort));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        sharedAccountActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}