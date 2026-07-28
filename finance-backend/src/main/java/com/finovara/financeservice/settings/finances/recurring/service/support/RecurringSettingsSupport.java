package com.finovara.financeservice.settings.finances.recurring.service.support;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RecurringSettingsSupport {

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RecurringSettings getSettings(Long userId, RecurringType type) {
        return recurringSettingsRepository.findByUserIdAndType(userId, type).orElseThrow(() -> new EntityNotFoundException("RecurringSettings not found for userId=" + userId + ", type=" + type));
    }

    public void applyCommonFields(Long userId, RecurringSettings settings, RecurringCommonFields fields, SettingType settingType) {
        boolean enabled = Boolean.TRUE.equals(fields.enable());
        settings.setEnable(enabled);
        settings.setAmount(fields.amount());
        settings.setPeriodType(fields.periodType());

        if (enabled) {
            settings.setStartDate(fields.startDate());
            settings.setEndDate(fields.endDate());
            settings.setNextExecutionDate(fields.startDate());
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, settingType, SettingActivityStatus.ENABLED, LocalDateTime.now()));
        } else {
            settings.setNextExecutionDate(null);
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, settingType, SettingActivityStatus.DISABLED, LocalDateTime.now()));
        }
    }
}

