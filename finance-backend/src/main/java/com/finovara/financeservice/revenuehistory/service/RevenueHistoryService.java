package com.finovara.financeservice.revenuehistory.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueHistoryService {
    private final FinancialPeriodService financialPeriodService;
    private final RevenueMapper revenueMapper;

    @Cacheable(value = "revenue:historyByCategory", key = "#userId + ':' + #periodType + ':' + #category")
    public List<RevenueDto> getRevenueByCategory(Long userId, PeriodType periodType, RevenueCategory category) {
        List<Revenue> revenues = financialPeriodService.getRevenuesInPeriodByCategory(userId, periodType, category);

        return revenues.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }
}
