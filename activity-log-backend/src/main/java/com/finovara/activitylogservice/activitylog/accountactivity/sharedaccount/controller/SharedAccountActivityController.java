package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.controller;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto.SharedAccountActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.service.SharedAccountActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}