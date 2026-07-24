package com.finovara.reportservice.report.finances.response.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.util.dto.ReportDto;
import com.finovara.reportservice.util.dto.financial.FinancialCashFlowDto;
import com.finovara.reportservice.util.dto.financial.FinancialCategoryPercentageDto;
import com.finovara.reportservice.util.dto.financial.FinancialPeriodSummaryDto;
import com.finovara.reportservice.util.dto.financial.FinancialReportDto;
import com.finovara.reportservice.util.dto.financial.FinancialTotalsDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.reportservice.report.finances.calculate.chart.averagecashflow.service.AverageCashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.chart.cashflow.service.TotalCashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestexpense.service.HighestExpenseService;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestrevenue.service.HighestRevenueService;
import com.finovara.reportservice.report.finances.calculate.sum.ReportSummaryService;
import com.finovara.reportservice.util.mapper.FinancialReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private final ReportSummaryService summaryService;
    private final HighestExpenseService highestExpenseService;
    private final HighestRevenueService highestRevenueService;
    private final ExpenseCategoryPercentageService expenseCategoryPercentageService;
    private final RevenueCategoryPercentageService revenueCategoryPercentageService;
    private final TotalCashFlowChartService totalCashFlowChartService;
    private final AverageCashFlowChartService averageCashFlowChartService;
    private final Clock clock;

    public FinancialReportDto getFinancialReport(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }

        LocalDate today = LocalDate.now(clock);

        FinancialTotalsDto totals = buildTotals(userId);
        FinancialPeriodSummaryDto periodSummary = buildPeriodSummary(userId);
        List<HighestExpenseDto> highestExpenses = highestExpenseService.getHighestExpense(userId, periodType);
        List<HighestRevenueDto> highestRevenues = highestRevenueService.getHighestRevenue(userId, periodType);
        List<FinancialCategoryPercentageDto> expensePercentages = buildExpensePercentages(userId, periodType);
        List<FinancialCategoryPercentageDto> revenuePercentages = buildRevenuePercentages(userId, periodType);
        List<FinancialCashFlowDto> cashFlow = FinancialReportMapper.mapCashFlow(totalCashFlowChartService.getCashFlowChart(userId, today));
        List<FinancialCashFlowDto> averageCashFlow = FinancialReportMapper.mapCashFlow(averageCashFlowChartService.getAverageCashFlowChart(userId, today));

        return new FinancialReportDto(
                periodType,
                totals,
                periodSummary,
                highestExpenses,
                highestRevenues,
                expensePercentages,
                revenuePercentages,
                cashFlow,
                averageCashFlow
        );
    }

    private FinancialTotalsDto buildTotals(Long userId) {
        BigDecimal expenses = summaryService.sumAllExpenses(userId);
        BigDecimal revenues = summaryService.sumAllRevenues(userId);
        return new FinancialTotalsDto(expenses, revenues, revenues.subtract(expenses));
    }

    private FinancialPeriodSummaryDto buildPeriodSummary(Long userId) {
        ReportDto dailyExpense = summaryService.sumExpense(userId, PeriodType.DAILY);
        ReportDto weeklyExpense = summaryService.sumExpense(userId, PeriodType.WEEKLY);
        ReportDto monthlyExpense = summaryService.sumExpense(userId, PeriodType.MONTHLY);
        ReportDto dailyRevenue = summaryService.sumRevenue(userId, PeriodType.DAILY);
        ReportDto weeklyRevenue = summaryService.sumRevenue(userId, PeriodType.WEEKLY);
        ReportDto monthlyRevenue = summaryService.sumRevenue(userId, PeriodType.MONTHLY);

        return new FinancialPeriodSummaryDto(
                dailyExpense,
                weeklyExpense,
                monthlyExpense,
                dailyRevenue,
                weeklyRevenue,
                monthlyRevenue
        );
    }

    private List<FinancialCategoryPercentageDto> buildExpensePercentages(Long userId, PeriodType periodType) {
        List<ExpenseCategoryPercentageDto> percentages = Arrays.stream(ExpenseCategory.values())
                .map(category -> expenseCategoryPercentageService.getExpensePercentageByCategoryReport(userId, category, periodType))
                .toList();
        return FinancialReportMapper.mapExpensePercentages(percentages);
    }

    private List<FinancialCategoryPercentageDto> buildRevenuePercentages(Long userId, PeriodType periodType) {
        List<RevenueCategoryPercentageDto> percentages = Arrays.stream(RevenueCategory.values())
                .map(category -> revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(userId, category, periodType))
                .toList();
        return FinancialReportMapper.mapRevenuePercentages(percentages);
    }
}