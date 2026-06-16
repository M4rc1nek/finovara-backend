package com.finovara.financeservice.settings;

import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceSettingsService {
    private final RecurringSettingsRepository recurringSettingsRepository;
    private final ExpenseSettingsRepository expenseSettingsRepository;

    @Transactional
    public void deleteRecurringSettings(Long userId) {
        recurringSettingsRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public void deleteExpenseSettings(Long userId) {
        expenseSettingsRepository.deleteByUserId(userId);
    }
}
