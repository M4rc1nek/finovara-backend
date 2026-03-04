package com.finovara.finovarabackend.accountactivity.settings.dto;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;

import java.time.LocalDateTime;

public record SettingsActivityDto(
        SettingActivityStatus status,
        SettingType settingType,
        LocalDateTime date
) {
}
