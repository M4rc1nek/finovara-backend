package com.finovara.finovarabackend.report.finances.categoryearned.service;

import com.finovara.finovarabackend.report.finances.categoryearned.dto.CategoryEarnedDto;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryEarnedService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public CategoryEarnedDto getCategoryEarnedReport(String email, RevenueCategory category, PeriodType periodType) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        BigDecimal summedRevenue = financialPeriodService.getEarned(user.getId(), periodType);
        List<Revenue> revenueCategory = financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal summedRevenueCategory = revenueCategory.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = BigDecimal.ZERO;

        if (summedRevenue.compareTo(BigDecimal.ZERO) > 0) {
            percentage = summedRevenueCategory
                    .multiply(BigDecimal.valueOf(100))
                    .divide(summedRevenue, 2, RoundingMode.HALF_UP);
        }
        return new CategoryEarnedDto(percentage, category);
    }
}
