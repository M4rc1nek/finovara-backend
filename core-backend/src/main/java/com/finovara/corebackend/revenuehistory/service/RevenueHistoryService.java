package com.finovara.corebackend.revenuehistory.service;

import com.finovara.corebackend.revenue.dto.RevenueDto;
import com.finovara.corebackend.revenue.mapper.RevenueMapper;
import com.finovara.corebackend.revenue.model.Revenue;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.corebackend.user.model.User;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueHistoryService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;
    private final RevenueMapper revenueMapper;

    public List<RevenueDto> getRevenueByCategory(Long userId, PeriodType periodType, RevenueCategory category) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        List<Revenue> revenues = financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, category);

        return revenues.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }
}
