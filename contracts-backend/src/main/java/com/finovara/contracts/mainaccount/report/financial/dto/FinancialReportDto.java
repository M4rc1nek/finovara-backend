package com.finovara.contracts.mainaccount.report.financial.dto;

import com.finovara.contracts.util.PeriodType;

import java.util.List;

public record FinancialReportDto(
        PeriodType periodType,
        FinancialTotalsDto totals,
        FinancialPeriodSummaryDto periodSummary,
        List<HighestExpenseDto> highestExpenses,
        List<HighestRevenueDto> highestRevenues,
        List<FinancialCategoryPercentageDto> expenseCategoryPercentages,
        List<FinancialCategoryPercentageDto> revenueCategoryPercentages,
        List<FinancialCashFlowDto> cashFlowChart,
        List<FinancialCashFlowDto> averageCashFlowChart
) {
}

