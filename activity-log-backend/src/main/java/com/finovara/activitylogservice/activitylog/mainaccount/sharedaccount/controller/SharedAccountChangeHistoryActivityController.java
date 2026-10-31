package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto.SharedAccountChangeHistoryActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto.SharedAccountChangeHistoryActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.service.SharedAccountChangeHistoryActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.SortType;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/account-activity/shared-account")
@RequiredArgsConstructor
public class SharedAccountChangeHistoryActivityController {

    private final SharedAccountChangeHistoryActivityService sharedAccountChangeHistoryActivityService;

    @GetMapping
    public ResponseEntity<List<SharedAccountChangeHistoryActivityDto>> getSharedAccountActivity(@RequestParam(defaultValue = "NEWEST") SortType sort, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(sharedAccountChangeHistoryActivityService.getSharedAccountActivity(SecurityUtils.getCurrentUserId(), sort, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SharedAccountChangeHistoryActivityDto>> filterSharedAccountActivity(@ModelAttribute SharedAccountChangeHistoryActivityFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(sharedAccountChangeHistoryActivityService.filterSharedAccountActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        sharedAccountChangeHistoryActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}