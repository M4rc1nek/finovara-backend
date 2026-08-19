package com.finovara.contracts.activity.event.settings;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;

import java.time.LocalDateTime;

public record SettingsActivityEvent(
        Long userId,
        SettingType settingType,
        SettingActivityStatus status,
        LocalDateTime occurredAt
) {
}