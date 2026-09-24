package com.finovara.activitylogservice.activitylog.securitymonitoring.controller;

import com.finovara.activitylogservice.activitylog.securitymonitoring.dto.RiskOperationActivityDto;
import com.finovara.activitylogservice.activitylog.securitymonitoring.service.RiskOperationLogService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/risk-operation")
@RequiredArgsConstructor
public class RiskOperationActivityController {

    private final RiskOperationLogService riskOperationLogService;

    @GetMapping
    public ResponseEntity<List<RiskOperationActivityDto>> getRiskActivity(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(riskOperationLogService.getRiskActivity(SecurityUtils.getCurrentUserId(), sort));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        riskOperationLogService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}
