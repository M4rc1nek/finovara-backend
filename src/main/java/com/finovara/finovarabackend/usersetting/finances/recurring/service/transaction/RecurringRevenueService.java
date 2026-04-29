package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringRevenueService {

    private final RecurringSettingsSupport recurringSettingsSupport;

    public void saveRevenueSettings(Long userId, RecurringRevenueDto dto) {

        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE);

        settings.setRevenueCategory(dto.revenueCategory());
        settings.setExpenseCategory(null);
        settings.setPiggyBankId(null);

        recurringSettingsSupport.applyCommonFields(
                settings,
                new RecurringCommonFields(dto.enable(), dto.amount(), dto.periodType(), dto.startDate()),
                userId,
                SettingType.REVENUE_RECURRING
        );
    }

    public RecurringRevenueDto getRevenueSettings(Long userId) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE);

        return new RecurringRevenueDto(
                settings.isEnable(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                settings.getPeriodType(),
                settings.getStartDate(),
                settings.getNextExecutionDate()
        );
    }
}

