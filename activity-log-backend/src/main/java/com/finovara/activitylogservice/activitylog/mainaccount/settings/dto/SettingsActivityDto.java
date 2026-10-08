package com.finovara.activitylogservice.activitylog.mainaccountactivity.settings.dto;

import com.finovara.contracts.mainaccount.activity.model.SettingActivityStatus;
import com.finovara.contracts.mainaccount.activity.model.SettingType;

import java.time.LocalDateTime;

public record SettingsActivityDto(
        SettingActivityStatus status,
        SettingType settingType,
        LocalDateTime createdAt
) {
}
