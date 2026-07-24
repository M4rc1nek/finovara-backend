package com.finovara.reportservice.util.dto.financial;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;

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

