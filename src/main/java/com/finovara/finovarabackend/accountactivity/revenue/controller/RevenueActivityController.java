package com.finovara.finovarabackend.accountactivity.revenue.controller;

import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/revenue")
@RequiredArgsConstructor
public class RevenueActivityController {

    private final RevenueActivityService revenueActivityService;

    @GetMapping
    public ResponseEntity<List<RevenueActivityDto>> getRevenueActivity(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(revenueActivityService.getRevenueActivity(SecurityUtils.getCurrentUserEmail(), sort));
    }
}