package com.finovara.finovarabackend.report.finances.categoryearned.service;

import com.finovara.finovarabackend.report.finances.categoryearned.dto.CategoryEarnedDto;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.calculate.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenuePercentageByCategory {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public CategoryEarnedDto getRevenuePercentageByCategoryReport(String email, RevenueCategory category, PeriodType periodType) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        BigDecimal summedRevenue = financialPeriodService.getRevenueSum(user.getId(), periodType);
        List<Revenue> revenueCategory = financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal summedRevenueCategory = revenueCategory.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = CalculatePercentage.calculatePercentage(summedRevenueCategory, summedRevenue);

        return new CategoryEarnedDto(percentage, category);
    }
}
