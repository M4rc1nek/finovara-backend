package com.finovara.reportservice.healthscore.controller;

import com.finovara.reportservice.healthscore.dto.HealthScoreDto;
import com.finovara.reportservice.healthscore.service.HealthScoreService;
import com.finovara.reportservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance-health-score")
public class HealthScoreController {
    private final HealthScoreService healthScoreService;

    @GetMapping
    public ResponseEntity<HealthScoreDto> getHealthScore() {
        return ResponseEntity.ok(healthScoreService.getHealthScore(SecurityUtils.getCurrentUserId()));
    }
}
