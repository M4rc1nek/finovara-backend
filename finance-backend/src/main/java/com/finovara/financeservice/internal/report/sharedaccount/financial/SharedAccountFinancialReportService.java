package com.finovara.financeservice.internal.report.sharedaccount.financial;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.financial.dto.*;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.internal.report.sharedaccount.InternalSharedReportDataService;
import com.finovara.financeservice.internal.report.util.CashFlowChartBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SharedAccountFinancialReportService {

    private final InternalSharedReportDataService internalSharedReportDataService;
    private final CashFlowChartBuilder cashFlowChartBuilder;

    @Value("${shared.expense.highest.page-size}")
    private int highestExpensesPageSize;

    @Value("${shared.revenue.highest.page-size}")
    private int highestRevenuesPageSize;

    public FinancialReportDto buildReport(Long ownerId, Long memberId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }

        LocalDate today = LocalDate.now();
        LocalDate from = periodType.getStartDate(today);

        List<DailyCashDto> expensesByDate = internalSharedReportDataService.expensesGroupedByDate(ownerId, memberId);
        List<DailyCashDto> revenuesByDate = internalSharedReportDataService.revenuesGroupedByDate(ownerId, memberId);

        return new FinancialReportDto(
                periodType,
                buildTotals(expensesByDate, revenuesByDate),
                buildPeriodSummary(expensesByDate, revenuesByDate, today),
                internalSharedReportDataService.highestExpenses(ownerId, memberId, from, today, highestExpensesPageSize),
                internalSharedReportDataService.highestRevenues(ownerId, memberId, from, today, highestRevenuesPageSize),
                buildExpensePercentages(ownerId, memberId, from, today),
                buildRevenuePercentages(ownerId, memberId, from, today),
                cashFlowChartBuilder.build(expensesByDate, revenuesByDate, today),
                cashFlowChartBuilder.build(internalSharedReportDataService.expensesAvgGroupedByDate(ownerId, memberId), internalSharedReportDataService.revenuesAvgGroupedByDate(ownerId, memberId), today));
    }

    private FinancialTotalsDto buildTotals(List<DailyCashDto> expenses, List<DailyCashDto> revenues) {
        BigDecimal expenseTotal = sum(expenses, LocalDate.MIN, LocalDate.MAX);
        BigDecimal revenueTotal = sum(revenues, LocalDate.MIN, LocalDate.MAX);
        return new FinancialTotalsDto(expenseTotal, revenueTotal, revenueTotal.subtract(expenseTotal));
    }

    private FinancialPeriodSummaryDto buildPeriodSummary(List<DailyCashDto> expenses, List<DailyCashDto> revenues, LocalDate today) {
        return new FinancialPeriodSummaryDto(
                period(expenses, PeriodType.DAILY, today),
                period(expenses, PeriodType.WEEKLY, today),
                period(expenses, PeriodType.MONTHLY, today),
                period(revenues, PeriodType.DAILY, today),
                period(revenues, PeriodType.WEEKLY, today),
                period(revenues, PeriodType.MONTHLY, today)
        );
    }

    private ReportDto period(List<DailyCashDto> daily, PeriodType type, LocalDate today) {
        return new ReportDto(type, sum(daily, type.getStartDate(today), today));
    }

    private BigDecimal sum(List<DailyCashDto> daily, LocalDate from, LocalDate to) {
        return daily.stream()
                .filter(day -> !day.date().isBefore(from) && !day.date().isAfter(to))
                .map(DailyCashDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<FinancialCategoryPercentageDto> buildExpensePercentages(Long ownerId, Long memberId, LocalDate from, LocalDate to) {
        Map<ExpenseCategory, BigDecimal> sums = internalSharedReportDataService.expenseSumsByCategory(ownerId, memberId, from, to);
        BigDecimal total = sums.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Arrays.stream(ExpenseCategory.values())
                .map(category -> new FinancialCategoryPercentageDto(
                        category.name(),
                        CalculatePercentage.calculatePercentage(sums.getOrDefault(category, BigDecimal.ZERO), total)))
                .toList();
    }

    private List<FinancialCategoryPercentageDto> buildRevenuePercentages(Long ownerId, Long memberId, LocalDate from, LocalDate to) {
        Map<RevenueCategory, BigDecimal> sums = internalSharedReportDataService.revenueSumsByCategory(ownerId, memberId, from, to);
        BigDecimal total = sums.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Arrays.stream(RevenueCategory.values())
                .map(category -> new FinancialCategoryPercentageDto(
                        category.name(),
                        CalculatePercentage.calculatePercentage(sums.getOrDefault(category, BigDecimal.ZERO), total)))
                .toList();
    }
}
