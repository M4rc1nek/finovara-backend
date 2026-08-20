package com.finovara.financeservice.internal.digest.report.email;

import com.finovara.contracts.notification.email.digest.report.WeeklyFinanceDigestReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/digest/weekly-email")
@RequiredArgsConstructor
public class InternalDigestReportEmailController {

    private final InternalDigestReportEmailService internalDigestReportEmailService;

    @GetMapping("/report")
    public ResponseEntity<List<WeeklyFinanceDigestReportDto>> getWeeklyFinanceDigestReports() {
        return ResponseEntity.ok(internalDigestReportEmailService.getWeeklyFinanceDigestReports());
    }
}