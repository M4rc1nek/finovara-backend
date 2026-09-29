package com.finovara.activitylogservice.internal.security.report;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.SecurityReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/activity/security/report")
@RequiredArgsConstructor
public class InternalSecurityReportController {

    private final InternalSecurityReportService internalSecurityReportService;

    @GetMapping
    public ResponseEntity<SecurityReportDto> getSecurityReport(@RequestHeader("X-User-Id") Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(internalSecurityReportService.getSummaryForUser(userId, periodType));
    }

}
