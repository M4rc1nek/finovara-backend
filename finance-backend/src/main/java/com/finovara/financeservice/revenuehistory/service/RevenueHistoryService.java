package com.finovara.authbackend.revenuehistory.service;

import com.finovara.authbackend.revenue.dto.RevenueDto;
import com.finovara.authbackend.revenue.mapper.RevenueMapper;
import com.finovara.authbackend.revenue.model.Revenue;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.authbackend.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueHistoryService {
    private final FinancialPeriodService financialPeriodService;
    private final RevenueMapper revenueMapper;

    public List<RevenueDto> getRevenueByCategory(Long userId, PeriodType periodType, RevenueCategory category) {
        List<Revenue> revenues = financialPeriodService.getRevenuesInPeriodByCategory(userId, periodType, category);

        return revenues.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }
}
