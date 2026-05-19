package com.finovara.finovarabackend.usersetting.finances.recurring.service.support;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringSettingsSupport {

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final SettingsActivityService settingsActivityService;

    public RecurringSettings getSettings(Long userId, RecurringType type) {
        return recurringSettingsRepository.findByUserAssignedIdAndType(userId, type)
                .orElseThrow(() -> new EntityNotFoundException("RecurringSettings not found for userId=" + userId + ", type=" + type));
    }

    public void applyCommonFields(Long userId, RecurringSettings settings, RecurringCommonFields fields, SettingType settingType) {
        boolean enabled = Boolean.TRUE.equals(fields.enable());
        settings.setEnable(enabled);
        settings.setAmount(fields.amount());
        settings.setPeriodType(fields.periodType());

        if (enabled) {
            settings.setStartDate(fields.startDate());
            settings.setNextExecutionDate(fields.startDate());
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.ENABLED, settingType);
        } else {
            settings.setNextExecutionDate(null);
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.DISABLED, settingType);
        }
    }
}

