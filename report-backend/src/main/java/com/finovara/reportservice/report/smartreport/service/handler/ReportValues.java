package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialTotalsDto;

import java.math.BigDecimal;
import java.util.Optional;

final class ReportValues {

    private ReportValues() {
    }

    static BigDecimal expenses(FinancialReportDto report) {
        return totals(report)
                .map(FinancialTotalsDto::expenses)
                .orElse(BigDecimal.ZERO);
    }

    static BigDecimal revenues(FinancialReportDto report) {
        return totals(report)
                .map(FinancialTotalsDto::revenues)
                .orElse(BigDecimal.ZERO);
    }

    private static Optional<FinancialTotalsDto> totals(FinancialReportDto report) {
        return Optional.ofNullable(report).map(FinancialReportDto::totals);
    }
}