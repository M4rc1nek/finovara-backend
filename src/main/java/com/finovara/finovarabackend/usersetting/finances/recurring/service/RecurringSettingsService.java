package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringSettingsService {

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final SettingsActivityService settingsActivityService;

    public void saveRevenueSettings(Long userId, RecurringSettingsDto dto) {

        RecurringSettings settings = recurringSettingsRepository.findByUserAssignedIdAndType(userId, RecurringType.REVENUE)
                .orElseThrow();

        settings.setEnable(dto.enable());
        settings.setAmount(dto.amount());
        settings.setRevenueCategory(dto.revenueCategory());
        settings.setPeriodType(dto.periodType());

        if (dto.enable()) {
            settings.setStartDate(dto.startDate());
            settings.setNextExecutionDate(dto.startDate());

            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.ENABLED, SettingType.REVENUE_RECURRING);
        } else {
            settings.setNextExecutionDate(null);
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.DISABLED, SettingType.REVENUE_RECURRING);
        }
    }

    public RecurringSettingsDto getRevenueSettings(Long userId) {
        RecurringSettings settings = recurringSettingsRepository.findByUserAssignedIdAndType(userId, RecurringType.REVENUE)
                .orElseThrow();

        return new RecurringSettingsDto(
                settings.isEnable(),
                settings.getAmount(),
                RecurringType.REVENUE,
                settings.getRevenueCategory(),
                settings.getExpenseCategory(),
                settings.getPeriodType(),
                settings.getStartDate(),
                settings.getNextExecutionDate()
        );
    }
}
