package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import org.springframework.stereotype.Component;

@Component
public class ExpenseRecurringSettingsHandler implements RecurringSettingsTypeHandler {

    @Override
    public RecurringType getType() {
        return RecurringType.EXPENSE;
    }

    @Override
    public SettingType getSettingType() {
        return SettingType.EXPENSE_RECURRING;
    }

    @Override
    public void applyTypeSpecificFields(RecurringSettings settings, RecurringSettingsDto dto) {
        settings.setExpenseCategory(dto.expenseCategory());
        settings.setRevenueCategory(null);
    }

    @Override
    public void validate(RecurringSettingsDto dto) {
        if (Boolean.TRUE.equals(dto.enable()) && dto.expenseCategory() == null) {
            throw new IllegalArgumentException("expenseCategory is required for EXPENSE");
        }
    }
}

