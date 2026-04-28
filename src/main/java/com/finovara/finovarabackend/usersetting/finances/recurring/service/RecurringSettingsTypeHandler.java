package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;

public interface RecurringSettingsTypeHandler {
    RecurringType getType();

    SettingType getSettingType();

    void applyTypeSpecificFields(RecurringSettings settings, RecurringSettingsDto dto);

    default void validate(RecurringSettingsDto dto) {
    }
}

