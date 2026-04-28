package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import org.springframework.stereotype.Component;

@Component
public class SavingsRecurringSettingsHandler implements RecurringSettingsTypeHandler {

    @Override
    public RecurringType getType() {
        return RecurringType.SAVINGS;
    }

    @Override
    public SettingType getSettingType() {
        return SettingType.SAVINGS_RECURRING;
    }

    @Override
    public void applyTypeSpecificFields(RecurringSettings settings, RecurringSettingsDto dto) {
        settings.setRevenueCategory(null);
        settings.setExpenseCategory(null);
    }
}

