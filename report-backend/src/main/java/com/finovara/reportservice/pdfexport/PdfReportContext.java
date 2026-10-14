package com.finovara.reportservice.pdfexport;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public class PdfReportContext {

    private final FinanceBackendReportClient reportClient;
    private final Long userId;
    private final PeriodType periodType;
    private final Map<PeriodType, FinancialReportDto> reports = new EnumMap<>(PeriodType.class);
    private BigDecimal walletBalance;

    public PdfReportContext(FinanceBackendReportClient reportClient, Long userId, PeriodType periodType) {
        this.reportClient = reportClient;
        this.userId = userId;
        this.periodType = periodType;
    }

    public PeriodType periodType() {
        return periodType;
    }

    public FinancialReportDto report() {
        return report(periodType);
    }

    public FinancialReportDto report(PeriodType type) {
        return reports.computeIfAbsent(type, t -> reportClient.getFinancialReport(userId, t));
    }

    public BigDecimal walletBalance() {
        if (walletBalance == null) {
            BigDecimal balance = reportClient.walletBalance(userId);
            walletBalance = balance != null ? balance : BigDecimal.ZERO;
        }
        return walletBalance;
    }
}