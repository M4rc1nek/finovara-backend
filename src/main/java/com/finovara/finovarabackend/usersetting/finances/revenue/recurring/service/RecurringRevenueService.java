package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
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
    private final SettingsActivityService settingsActivityService;

    @Transactional
    public void saveRecurringRevenue(String email, RecurringRevenueDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        RevenueSettings revenueSettings = user.getRevenueSettings();

        revenueSettings.setRecurringRevenuesEnable(dto.recurringRevenueEnable());
        revenueSettings.setRecurringAmount(dto.amount());
        revenueSettings.setRevenueCategory(dto.category());
        revenueSettings.setPeriodType(dto.periodType());

        if (dto.recurringRevenueEnable()) {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.REVENUE_RECURRING);
            revenueSettings.setRecurringStartDate(dto.startDate());
            revenueSettings.setNextExecutionDate(dto.startDate());
        } else {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.REVENUE_RECURRING);
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
                settings.getPeriodType(),
                settings.getRecurringStartDate(),
                settings.getNextExecutionDate()
        );
    }
}

