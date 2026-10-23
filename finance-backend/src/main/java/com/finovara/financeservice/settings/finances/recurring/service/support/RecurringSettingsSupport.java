package com.finovara.financeservice.settings.finances.recurring.service.support;

import com.finovara.contracts.mainaccount.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.SettingActivityStatus;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RecurringSettingsSupport {

    private static final String SETTINGS_CHANGED_TOPIC = "settings.changed";

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final OutboxService outboxService;

    public RecurringSettings getSettings(Long userId, RecurringType type) {
        return recurringSettingsRepository.findByUserIdAndType(userId, type).orElseThrow(() -> notFound(userId, type));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public RecurringSettings getSettingsForUpdate(Long userId, RecurringType type) {
        return recurringSettingsRepository.findByUserIdAndTypeForUpdate(userId, type).orElseThrow(() -> notFound(userId, type));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void applyCommonFields(Long userId, RecurringSettings settings, RecurringCommonFields fields, SettingType settingType) {
        boolean enabled = Boolean.TRUE.equals(fields.enable());
        settings.setEnable(enabled);
        settings.setAmount(fields.amount());
        settings.setPeriodType(fields.periodType());

        if (enabled) {
            settings.setStartDate(fields.startDate());
            settings.setEndDate(fields.endDate());
            settings.setNextExecutionDate(fields.startDate());
        } else {
            settings.setNextExecutionDate(null);
        }

        outboxService.save("RecurringSettings", settings.getId().toString(), SETTINGS_CHANGED_TOPIC,
                new SettingsActivityEvent(userId, settingType,
                        enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }

    private EntityNotFoundException notFound(Long userId, RecurringType type) {
        return new EntityNotFoundException("RecurringSettings not found for userId=" + userId + ", type=" + type);
    }
}