package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.dto.SharedRevenueCategoryPercentageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SharedRevenueCategoryPercentageService {

    private final FinanceBackendSharedReportClient reportClient;

    @Cacheable(value = "report:sharedRevenuePercentageByCategory", key = "#ownerId + ':' + #memberId + ':' + #category + ':' + #periodType")
    public SharedRevenueCategoryPercentageDto getRevenuePercentageByCategoryReport(Long ownerId, Long memberId, RevenueCategory category, PeriodType periodType) {

        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);

        BigDecimal total = reportClient.sumRevenues(ownerId, memberId, from, to);
        BigDecimal inCategory = reportClient.revenuesByCategory(ownerId, memberId, from, to, category);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(inCategory, total);

        return new SharedRevenueCategoryPercentageDto(percentage, category);
    }
}