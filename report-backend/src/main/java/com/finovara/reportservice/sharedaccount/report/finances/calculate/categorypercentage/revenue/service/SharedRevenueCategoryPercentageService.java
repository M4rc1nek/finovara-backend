package com.finovara.reportservice.sharedaccount.report.finances.categorypercentage.revenue.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.categorypercentage.revenue.dto.SharedRevenueCategoryPercentageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SharedRevenueCategoryPercentageService {

    private final FinanceBackendSharedReportClient reportClient;

    @Cacheable(value = "report:sharedRevenuePercentageByCategory", key = "#userId + ':' + #category + ':' + #periodType")
    public SharedRevenueCategoryPercentageDto getRevenuePercentageByCategoryReport(Long userId, RevenueCategory category, PeriodType periodType) {

        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);

        BigDecimal total = reportClient.sumRevenues(userId, from, to);
        BigDecimal inCategory = reportClient.revenuesByCategory(userId, from, to, category);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(inCategory, total);

        return new SharedRevenueCategoryPercentageDto(percentage, category);
    }
}