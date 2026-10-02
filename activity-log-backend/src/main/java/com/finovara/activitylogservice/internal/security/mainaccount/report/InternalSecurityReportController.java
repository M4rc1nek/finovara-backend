package com.finovara.activitylogservice.internal.security.mainaccount.report;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.mainaccount.report.security.dto.SecurityReportDto;
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
