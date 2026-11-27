package com.finovara.reportservice.savingsplan.controller;

import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.response.SavingsPlanReport;
import com.finovara.reportservice.savingsplan.service.SavingsPlanService;
import com.finovara.reportservice.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/savings-plan")
@RequiredArgsConstructor
public class SavingsPlanController {

    private final SavingsPlanService savingsPlanService;

    @PostMapping
    public ResponseEntity<SavingsPlanReport> createReport(@Valid @RequestBody SavingsPlanRequest request) {
        return ResponseEntity.ok(savingsPlanService.createReport(SecurityUtils.getCurrentUserId(), request));
    }
}
