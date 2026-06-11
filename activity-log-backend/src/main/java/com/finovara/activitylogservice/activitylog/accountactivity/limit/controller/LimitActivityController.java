package com.finovara.activityservice.activitylog.accountactivity.limit.controller;

import com.finovara.activityservice.activitylog.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.limit.service.LimitActivityService;
import com.finovara.activityservice.security.SecurityUtils;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/limit")
@RequiredArgsConstructor
public class LimitActivityController {

    private final LimitActivityService limitActivityService;

    @GetMapping
    public ResponseEntity<List<LimitActivityDto>> getLimitActivity(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(limitActivityService.getLimitActivity(SecurityUtils.getCurrentUserId(), sort));
    }

}
