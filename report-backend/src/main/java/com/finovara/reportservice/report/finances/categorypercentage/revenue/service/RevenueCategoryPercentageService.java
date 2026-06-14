package com.finovara.reportservice.report.finances.categorypercentage.revenue.service;

import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RevenueCategoryPercentageService {

    private final FinanceBackendReportClient reportClient;

    public RevenueCategoryPercentageDto getRevenuePercentageByCategoryReport(
            Long userId, RevenueCategory category, PeriodType periodType) {

        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);

        BigDecimal total = reportClient.sumRevenues(userId, from, to);
        BigDecimal inCategory = reportClient.revenuesByCategory(userId, from, to, category);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(inCategory, total);

        return new RevenueCategoryPercentageDto(percentage, category);
    }
}