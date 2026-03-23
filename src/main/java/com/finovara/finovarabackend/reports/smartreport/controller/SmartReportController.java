package com.finovara.finovarabackend.reports.smartreport.controller;

import com.finovara.finovarabackend.reports.smartreport.service.SmartReportService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/reports/smart-report")
@RequiredArgsConstructor
public class SmartReportController {

    private final SmartReportService smartReportService;

    @PostMapping
    public ResponseEntity<String> askQuestion(@RequestParam String userQuestion) {
        return ResponseEntity.ok(smartReportService.generateResponse(SecurityUtils.getCurrentUserEmail(), userQuestion));
    }
}
