package com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.service;

import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.contracts.util.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RevenueCategoryPercentageService {

    private final FinanceBackendReportClient reportClient;

    @Cacheable(value = "financial-report:revenuePercentageByCategory", key = "#userId + ':' + #category + ':' + #periodType")
    public RevenueCategoryPercentageDto getRevenuePercentageByCategoryReport(Long userId, RevenueCategory category, PeriodType periodType) {

        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);

        BigDecimal total = reportClient.sumRevenues(userId, from, to);
        BigDecimal inCategory = reportClient.revenuesByCategory(userId, from, to, category);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(inCategory, total);

        return new RevenueCategoryPercentageDto(percentage, category);
    }
}