package com.finovara.activityservice.contracts.event.settings;

import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.model.activity.SettingType;

import java.time.LocalDateTime;

public record SettingsActivityEvent(
        Long userId,
        SettingType settingType,
        SettingActivityStatus status,
        LocalDateTime occurredAt
) {
}