package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringSavingsService {

    private final RecurringSettingsSupport recurringSettingsSupport;
    private final RecurringSavingsValidator recurringSavingsValidator;
    private final WalletManagerService walletManagerService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void saveSavingsSettings(Long userId, RecurringSavingsDto recurringSavingsDto) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(recurringSavingsDto.authorizationCode()));

        RecurringSettings settings = recurringSettingsSupport.getSettingsForUpdate(userId, RecurringType.SAVINGS);
        settings.setPiggyBankId(recurringSavingsDto.piggyBankId());
        settings.setRevenueCategory(null);
        settings.setExpenseCategory(null);

        recurringSettingsSupport.applyCommonFields(
                userId,
                settings,
                new RecurringCommonFields(recurringSavingsDto.enable(), recurringSavingsDto.amount(), recurringSavingsDto.periodType(), recurringSavingsDto.startDate(), recurringSavingsDto.endDate()),
                SettingType.SAVINGS_RECURRING
        );

        if (settings.isEnable()) {
            Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
            recurringSavingsValidator.validate(settings, wallet);
        }
    }

    @Transactional(readOnly = true)
    public RecurringSavingsDto getSavingsSettings(Long userId) {
        RecurringSettings settings = recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS);

        return new RecurringSavingsDto(
                settings.isEnable(),
                settings.getAmount(),
                settings.getPiggyBankId(),
                settings.getPeriodType(),
                settings.getStartDate(),
                settings.getEndDate(),
                settings.getNextExecutionDate(),
                null
        );
    }
}