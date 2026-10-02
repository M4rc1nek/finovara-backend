package com.finovara.contracts.mainaccount.activity.event.settings;

import com.finovara.contracts.mainaccount.activity.model.SettingActivityStatus;
import com.finovara.contracts.mainaccount.activity.model.SettingType;

import java.time.LocalDateTime;

public record SettingsActivityEvent(
        Long userId,
        SettingType settingType,
        SettingActivityStatus status,
        LocalDateTime occurredAt
) {
}