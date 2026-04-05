package com.finovara.finovarabackend.report.finances.categorypercentage.revenue.service;

import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueCategoryPercentageService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public RevenueCategoryPercentageDto getRevenuePercentageByCategoryReport(String email, RevenueCategory category, PeriodType periodType) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        BigDecimal totalRevenue = financialPeriodService.getRevenueSum(user.getId(), periodType);
        List<Revenue> revenuesInCategory = financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal totalRevenueInCategory = revenuesInCategory.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = CalculatePercentage.calculatePercentage(totalRevenueInCategory, totalRevenue);

        return new RevenueCategoryPercentageDto(percentage, category);
    }
}
