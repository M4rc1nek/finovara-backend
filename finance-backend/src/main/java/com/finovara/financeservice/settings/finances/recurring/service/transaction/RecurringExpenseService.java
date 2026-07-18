package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringExpenseDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private final RecurringSettingsSupport recurringSettingsSupport;
    private final RecurringExpenseValidator recurringExpenseValidator;
    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final WalletManagerService walletManagerService;
    private final LimitManagerService limitManagerService;

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
            ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);
            Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
            List<Limit> limits = limitManagerService.getLimitsByUserId(userId);

            recurringExpenseValidator.validate(settings, expenseSettings, wallet, limits);
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
