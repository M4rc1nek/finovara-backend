package com.finovara.activitylogservice.activitylog.mainaccount.settings.dto;

import com.finovara.contracts.mainaccount.activity.model.SettingActivityStatus;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record SettingsActivityFilterDto(
       SettingActivityStatus status,
       SettingType settingType,
       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {}
