package com.finovara.financeservice.settings.finances.expense.quantitylimit.service;

import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.financeservice.util.settings.SettingsManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CountQuantityLimitEmergencyModeService {

    private final SettingsManager settingsManager;

    @Transactional
    public void saveEmergencyMode(Long userId, CountQuantityLimitEmergencyModeDto dto) {
        ExpenseSettings expenseSettings = settingsManager.getExpenseSettingsByUserId(userId);

        expenseSettings.setQuantityLimitEmergencyModeEnabled(dto.expenseQuantityLimitEmergencyModeEnabled());
    }
}

