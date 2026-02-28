package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringRevenueService {

    private final UserManagerService userManagerService;

    @Transactional
    public void saveRecurringRevenue(String email, RecurringRevenueDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        RevenueSettings revenueSettings = user.getRevenueSettings();

        revenueSettings.setRecurringRevenuesEnable(dto.recurringRevenueEnable());
        revenueSettings.setRecurringAmount(dto.amount());
        revenueSettings.setRevenueCategory(dto.category());
        revenueSettings.setRecurringStrategy(dto.strategy());

        if (dto.recurringRevenueEnable()) {
            revenueSettings.setRecurringStartDate(dto.startDate());
            revenueSettings.setNextExecutionDate(dto.startDate());
        } else {
            revenueSettings.setNextExecutionDate(null);
        }

    }

    public RecurringRevenueDto getRecurringRevenue(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        RevenueSettings settings = user.getRevenueSettings();

        return new RecurringRevenueDto(
                settings.isRecurringRevenuesEnable(),
                settings.getRecurringAmount(),
                settings.getRevenueCategory(),
                settings.getRecurringStrategy(),
                settings.getRecurringStartDate(),
                settings.getNextExecutionDate()
        );
    }
}

