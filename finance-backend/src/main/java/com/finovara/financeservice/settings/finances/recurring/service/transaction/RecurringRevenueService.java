package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.model.activity.SettingType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringRevenueDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.contracts.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import lombok.RequiredArgsConstructor;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringRevenueService {

    private final RecurringSettingsSupport recurringSettingsSupport;
    private final RecurringRevenueValidator recurringRevenueValidator;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void saveRevenueSettings(Long userId, RecurringRevenueDto dto) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE);
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));

        settings.setRevenueCategory(dto.revenueCategory());
        settings.setExpenseCategory(null);
        settings.setPiggyBankId(null);

        recurringSettingsSupport.applyCommonFields(
                userId,
                settings,
                new RecurringCommonFields(dto.enable(), dto.amount(), dto.periodType(), dto.startDate(), dto.endDate()),
                SettingType.REVENUE_RECURRING
        );

        if (settings.isEnable()) {
            recurringRevenueValidator.validate(settings);
        }
    }

    public RecurringRevenueDto getRevenueSettings(Long userId) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE);

        return new RecurringRevenueDto(
                settings.isEnable(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                settings.getPeriodType(),
                settings.getStartDate(),
                settings.getEndDate(),
                settings.getNextExecutionDate(),
                null
        );
    }
}

