package com.finovara.financeservice.internal.digest.report.email.service;

import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RevenueDigestService {

    private final RevenueRepository revenueRepository;

    public RevenueSummary calculateSummary(Long userId, LocalDate from, LocalDate to) {
        BigDecimal sum = calculateSum(userId, from, to);
        Optional<Revenue> highestExpense = findHighestRevenue(userId, from, to);
        String topCategory = findTopCategory(userId, from, to);

        return new RevenueSummary(sum, topCategory,
                highestExpense.map(Revenue::getAmount).orElse(BigDecimal.ZERO),
                highestExpense.map(e -> e.getCategory().name()).orElse(null),
                highestExpense.map(Revenue::getCreatedAt).orElse(null));
    }

    private BigDecimal calculateSum(Long userId, LocalDate from, LocalDate to) {
        return revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    private String findTopCategory(Long userId, LocalDate from, LocalDate to) {
        return revenueRepository
                .findTopRevenueCategoriesByUserIdAndPeriod(userId, from, to, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(RevenueCategory::name)
                .orElse(null);
    }

    private Optional<Revenue> findHighestRevenue(Long userId, LocalDate from, LocalDate to) {
        return revenueRepository
                .findTopRevenuesByUserIdAndPeriod(userId, from, to, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }
}