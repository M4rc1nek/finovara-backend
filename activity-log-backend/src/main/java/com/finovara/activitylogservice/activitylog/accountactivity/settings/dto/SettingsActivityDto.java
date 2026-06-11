package com.finovara.activitylogservice.activitylog.accountactivity.settings.dto;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;

import java.time.LocalDateTime;

public record SettingsActivityDto(
        SettingActivityStatus status,
        SettingType settingType,
        LocalDateTime createdAt
) {
}
