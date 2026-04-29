package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringSavingsService {

    private final RecurringSettingsSupport recurringSettingsSupport;

    public void saveSavingsSettings(Long userId, RecurringSavingsDto dto) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS);

        if (dto.enable() && dto.piggyBankId() == null) {
            throw new MissingRequirementException("Piggy bank id is required for recurring savings");
        }

        settings.setPiggyBankId(dto.piggyBankId());
        settings.setRevenueCategory(null);
        settings.setExpenseCategory(null);

        recurringSettingsSupport.applyCommonFields(
                settings,
                new RecurringCommonFields(dto.enable(), dto.amount(), dto.periodType(), dto.startDate()),
                userId,
                SettingType.SAVINGS_RECURRING
        );
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

