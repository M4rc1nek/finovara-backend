package com.finovara.corebackend.usersetting.finances.recurring.service.transaction;

import com.finovara.contracts.model.activity.SettingType;
import com.finovara.corebackend.usersetting.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.corebackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import com.finovara.corebackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.corebackend.wallet.model.Wallet;
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

