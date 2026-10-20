package com.finovara.financeservice.util.settings;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsManager {
    private final ExpenseSettingsRepository expenseSettingsRepository;

    public ExpenseSettings getExpenseSettingsByUserId(Long userId) {
        return expenseSettingsRepository.findByUserId(userId).
                orElseThrow(() -> new RequestedEntityNotFoundException("Expense settings not found"));
    }
}

