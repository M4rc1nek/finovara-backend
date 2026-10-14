package com.finovara.financeservice.internal.report.sharedaccount;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.internal.report.sharedaccount.financial.SharedAccountFinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/shared/reports")
@RequiredArgsConstructor
public class InternalSharedReportDataController {

    private final SharedAccountFinancialReportService sharedAccountFinancialReportService;
    private final InternalSharedReportDataService internalSharedReportDataService;

    @GetMapping("/financial-report")
    public ResponseEntity<FinancialReportDto> financialReport(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(sharedAccountFinancialReportService.buildReport(ownerId, memberId, periodType));
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<BigDecimal> walletBalance(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.walletBalance(ownerId, memberId));
    }
}