package com.finovara.finovarabackend.revenuehistory.service;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.mapper.RevenueMapper;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueHistoryService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;
    private final RevenueMapper revenueMapper;

    public List<RevenueDTO> getRevenueByCategory(String email, PeriodType periodType, RevenueCategory category) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        List<Revenue> revenues = financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, category);

        return revenues.stream()
                .map(revenueMapper::mapRevenueToDTO)
                .toList();
    }
}
