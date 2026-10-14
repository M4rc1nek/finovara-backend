package com.finovara.contracts.mainaccount.report.financial.dto;

public record FinancialPeriodSummaryDto(
        ReportDto dailyExpense,
        ReportDto weeklyExpense,
        ReportDto monthlyExpense,
        ReportDto dailyRevenue,
        ReportDto weeklyRevenue,
        ReportDto monthlyRevenue
) {
}

