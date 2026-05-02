package com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.finovarabackend.wallet.model.Wallet;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringSavingsService {

    private final RecurringSettingsSupport recurringSettingsSupport;
    private final RecurringSavingsValidator recurringSavingsValidator;

    @Transactional
    public void saveSavingsSettings(Long userId, RecurringSavingsDto dto) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS);

        settings.setPiggyBankId(dto.piggyBankId());
        settings.setRevenueCategory(null);
        settings.setExpenseCategory(null);

        recurringSettingsSupport.applyCommonFields(
                userId,
                settings,
                new RecurringCommonFields(dto.enable(), dto.amount(), dto.periodType(), dto.startDate()),
                SettingType.SAVINGS_RECURRING
        );

        if (settings.isEnable()) {
            Wallet wallet = settings.getUserAssigned().getWallet();
            recurringSavingsValidator.validate(settings, wallet);
        }
    }

    public RecurringSavingsDto getSavingsSettings(Long userId) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS);

        return new RecurringSavingsDto(
                settings.isEnable(),
                settings.getAmount(),
                settings.getPiggyBankId(),
                settings.getPeriodType(),
                settings.getStartDate(),
                settings.getNextExecutionDate()
        );
    }
}

