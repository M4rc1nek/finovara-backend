package com.finovara.finovarabackend.accountactivity.limit.controller;

import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
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
        return ResponseEntity.ok(limitActivityService.getLimitActivity(SecurityUtils.getCurrentUserEmail(), sort));
    }

}
