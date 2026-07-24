package com.finovara.reportservice.sharedaccount.report.finances.response.service;

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
import com.finovara.reportservice.util.mapper.FinancialReportMapper;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto.SharedExpenseCategoryPercentageDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.service.SharedExpenseCategoryPercentageService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.dto.SharedRevenueCategoryPercentageDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.service.SharedRevenueCategoryPercentageService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.averagecashflow.service.SharedAverageCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.cashflow.service.SharedTotalCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.highesttransactions.highestexpense.service.SharedHighestExpenseService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.highesttransactions.highestrevenue.service.SharedHighestRevenueService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.sum.service.SharedReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedFinancialReportService {

    private final SharedReportSummaryService summaryService;
    private final SharedHighestExpenseService highestExpenseService;
    private final SharedHighestRevenueService highestRevenueService;
    private final SharedExpenseCategoryPercentageService expenseCategoryPercentageService;
    private final SharedRevenueCategoryPercentageService revenueCategoryPercentageService;
    private final SharedTotalCashFlowChartService totalCashFlowChartService;
    private final SharedAverageCashFlowChartService averageCashFlowChartService;

    public FinancialReportDto getFinancialReport(Long ownerId, Long memberId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }

        FinancialTotalsDto totals = buildTotals(ownerId, memberId);
        FinancialPeriodSummaryDto periodSummary = buildPeriodSummary(ownerId, memberId);
        List<HighestExpenseDto> highestExpenses = highestExpenseService.getHighestExpense(ownerId, memberId, periodType);
        List<HighestRevenueDto> highestRevenues = highestRevenueService.getHighestRevenue(ownerId, memberId, periodType);
        List<FinancialCategoryPercentageDto> expensePercentages = buildExpensePercentages(ownerId, memberId, periodType);
        List<FinancialCategoryPercentageDto> revenuePercentages = buildRevenuePercentages(ownerId, memberId, periodType);
        List<FinancialCashFlowDto> cashFlow = FinancialReportMapper.mapSharedCashFlow(totalCashFlowChartService.getCashFlowChart(ownerId, memberId));
        List<FinancialCashFlowDto> averageCashFlow = FinancialReportMapper.mapSharedCashFlow(averageCashFlowChartService.getAverageCashFlowChart(ownerId, memberId));

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

    private FinancialTotalsDto buildTotals(Long ownerId, Long memberId) {
        BigDecimal expenses = summaryService.sumAllExpenses(ownerId, memberId);
        BigDecimal revenues = summaryService.sumAllRevenues(ownerId, memberId);
        return new FinancialTotalsDto(expenses, revenues, revenues.subtract(expenses));
    }

    private FinancialPeriodSummaryDto buildPeriodSummary(Long ownerId, Long memberId) {
        ReportDto dailyExpense = summaryService.sumExpense(ownerId, memberId, PeriodType.DAILY);
        ReportDto weeklyExpense = summaryService.sumExpense(ownerId, memberId, PeriodType.WEEKLY);
        ReportDto monthlyExpense = summaryService.sumExpense(ownerId, memberId, PeriodType.MONTHLY);
        ReportDto dailyRevenue = summaryService.sumRevenue(ownerId, memberId, PeriodType.DAILY);
        ReportDto weeklyRevenue = summaryService.sumRevenue(ownerId, memberId, PeriodType.WEEKLY);
        ReportDto monthlyRevenue = summaryService.sumRevenue(ownerId, memberId, PeriodType.MONTHLY);

        return new FinancialPeriodSummaryDto(
                dailyExpense,
                weeklyExpense,
                monthlyExpense,
                dailyRevenue,
                weeklyRevenue,
                monthlyRevenue
        );
    }

    private List<FinancialCategoryPercentageDto> buildExpensePercentages(Long ownerId, Long memberId, PeriodType periodType) {
        List<SharedExpenseCategoryPercentageDto> percentages = Arrays.stream(ExpenseCategory.values())
                .map(category -> expenseCategoryPercentageService.getExpensePercentageByCategoryReport(ownerId, memberId, category, periodType))
                .toList();
        return FinancialReportMapper.mapSharedExpensePercentages(percentages);
    }

    private List<FinancialCategoryPercentageDto> buildRevenuePercentages(Long ownerId, Long memberId, PeriodType periodType) {
        List<SharedRevenueCategoryPercentageDto> percentages = Arrays.stream(RevenueCategory.values())
                .map(category -> revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(ownerId, memberId, category, periodType))
                .toList();
        return FinancialReportMapper.mapSharedRevenuePercentages(percentages);
    }
}