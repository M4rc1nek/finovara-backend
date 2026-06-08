package com.finovara.corebackend.usersetting.finances.recurring.service.transaction;

import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.usersetting.finances.recurring.dto.RecurringExpenseDto;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.corebackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import com.finovara.corebackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.corebackend.wallet.model.Wallet;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private final RecurringSettingsSupport recurringSettingsSupport;
    private final RecurringExpenseValidator recurringExpenseValidator;

    @Transactional
    public void saveExpenseSettings(Long userId, RecurringExpenseDto dto) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE);

        settings.setExpenseCategory(dto.expenseCategory());
        settings.setRevenueCategory(null);
        settings.setPiggyBankId(null);

        recurringSettingsSupport.applyCommonFields(
                userId,
                settings,
                new RecurringCommonFields(dto.enable(), dto.amount(), dto.periodType(), dto.startDate()),
                SettingType.EXPENSE_RECURRING
        );

        if (settings.isEnable()) {
            ExpenseSettings expenseSettings = settings.getUserAssigned().getExpenseSettings();
            Wallet wallet = settings.getUserAssigned().getWallet();

            recurringExpenseValidator.validate(settings, expenseSettings, wallet);
        }
    }

    public RecurringExpenseDto getExpenseSettings(Long userId) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE);

        return new RecurringExpenseDto(
                settings.isEnable(),
                settings.getAmount(),
                settings.getExpenseCategory(),
                settings.getPeriodType(),
                settings.getStartDate(),
                settings.getNextExecutionDate()
        );
    }
}

