package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import org.springframework.stereotype.Component;

@Component
public class RevenueRecurringSettingsHandler implements RecurringSettingsTypeHandler {

    @Override
    public RecurringType getType() {
        return RecurringType.REVENUE;
    }

    @Override
    public SettingType getSettingType() {
        return SettingType.REVENUE_RECURRING;
    }

    @Override
    public void applyTypeSpecificFields(RecurringSettings settings, RecurringSettingsDto dto) {
        settings.setRevenueCategory(dto.revenueCategory());
        settings.setExpenseCategory(null);
    }

    @Override
    public void validate(RecurringSettingsDto dto) {
        if (Boolean.TRUE.equals(dto.enable()) && dto.revenueCategory() == null) {
            throw new IllegalArgumentException("revenueCategory is required for REVENUE");
        }
    }
}

