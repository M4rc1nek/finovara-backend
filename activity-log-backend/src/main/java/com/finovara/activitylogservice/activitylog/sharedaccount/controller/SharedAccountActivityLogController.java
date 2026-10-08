package com.finovara.activitylogservice.activitylog.sharedaccount.controller;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.service.SharedAccountActivityLogService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shared-accounts/activity")
@RequiredArgsConstructor
public class SharedAccountActivityLogController {

    private final SharedAccountActivityLogService sharedAccountActivityLogService;

    @GetMapping
    public ResponseEntity<List<SharedAccountActivityLogDto>> getSharedAccountActivityLog(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(sharedAccountActivityLogService.getSharedAccountLogActivity(SecurityUtils.getCurrentUserId(), sort));
    }

}
