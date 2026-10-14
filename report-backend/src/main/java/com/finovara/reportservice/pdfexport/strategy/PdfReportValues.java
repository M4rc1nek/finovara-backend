package com.finovara.reportservice.pdfexport.strategy;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialPeriodSummaryDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialTotalsDto;
import com.finovara.contracts.mainaccount.report.financial.dto.ReportDto;
import com.finovara.contracts.util.PeriodType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PdfReportValues {

    private PdfReportValues() {
    }

    public static BigDecimal revenues(FinancialReportDto report) {
        return Optional.ofNullable(report.totals())
                .map(FinancialTotalsDto::revenues)
                .orElse(BigDecimal.ZERO);
    }

    public static BigDecimal expenses(FinancialReportDto report) {
        return Optional.ofNullable(report.totals())
                .map(FinancialTotalsDto::expenses)
                .orElse(BigDecimal.ZERO);
    }

    public static BigDecimal revenueFor(FinancialReportDto report, PeriodType period) {
        return summaryAmount(report, summary -> switch (period) {
            case DAILY -> summary.dailyRevenue();
            case WEEKLY -> summary.weeklyRevenue();
            case MONTHLY -> summary.monthlyRevenue();
        });
    }

    public static BigDecimal expenseFor(FinancialReportDto report, PeriodType period) {
        return summaryAmount(report, summary -> switch (period) {
            case DAILY -> summary.dailyExpense();
            case WEEKLY -> summary.weeklyExpense();
            case MONTHLY -> summary.monthlyExpense();
        });
    }

    private static BigDecimal summaryAmount(FinancialReportDto report,
                                            Function<FinancialPeriodSummaryDto, ReportDto> selector) {
        return Optional.ofNullable(report.periodSummary())
                .map(selector)
                .map(ReportDto::amount)
                .orElse(BigDecimal.ZERO);
    }

    public static BigDecimal averagePerDay(BigDecimal total, PeriodType period) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(period.getStartDate(today), today) + 1;
        return total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    public static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}