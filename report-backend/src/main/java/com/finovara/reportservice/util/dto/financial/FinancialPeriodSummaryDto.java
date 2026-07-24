package com.finovara.reportservice.util.dto.financial;

import com.finovara.reportservice.util.dto.ReportDto;

public record FinancialPeriodSummaryDto(
        ReportDto dailyExpense,
        ReportDto weeklyExpense,
        ReportDto monthlyExpense,
        ReportDto dailyRevenue,
        ReportDto weeklyRevenue,
        ReportDto monthlyRevenue
) {
}

