package com.finovara.corebackend.report.finances.categorypercentage.revenue.service;

import com.finovara.corebackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.corebackend.revenue.model.Revenue;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.corebackend.user.model.User;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.percentage.CalculatePercentage;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueCategoryPercentageService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public RevenueCategoryPercentageDto getRevenuePercentageByCategoryReport(Long userId, RevenueCategory category, PeriodType periodType) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        BigDecimal totalRevenue = financialPeriodService.getRevenueSum(user.getId(), periodType);
        List<Revenue> revenuesInCategory = financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal totalRevenueInCategory = revenuesInCategory.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = CalculatePercentage.calculatePercentage(totalRevenueInCategory, totalRevenue);

        return new RevenueCategoryPercentageDto(percentage, category);
    }
}
