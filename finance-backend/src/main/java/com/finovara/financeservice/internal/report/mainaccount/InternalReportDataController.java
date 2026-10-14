package com.finovara.financeservice.internal.mainaccount;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.internal.mainaccount.financial.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportDataController {

    private final FinancialReportService financialReportService;
    private final InternalReportDataService internalReportDataService;

    @GetMapping("/financial-report")
    public ResponseEntity<FinancialReportDto> financialReport(@RequestParam Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(financialReportService.buildReport(userId, periodType));
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<BigDecimal> walletBalance(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.walletBalance(userId));
    }
}