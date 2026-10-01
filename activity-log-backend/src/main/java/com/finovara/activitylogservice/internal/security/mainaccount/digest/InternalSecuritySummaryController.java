package com.finovara.activitylogservice.internal.security.digest;

import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/activity/security/digest/weekly-email")
@RequiredArgsConstructor
public class InternalSecuritySummaryController {

    private final InternalSecurityDigestService internalSecurityDigestService;

    @GetMapping("/log-summary")
    public ResponseEntity<List<WeeklySecurityDigestReportDto>> getSecuritySummary(){
        return ResponseEntity.ok(internalSecurityDigestService.getSecurityDigestReport());
    }
}
