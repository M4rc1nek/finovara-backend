package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPaymentsService {

    private final UserManagerService userManagerService;
    private final WalletManagerService walletManagerService;

    private final PiggyBankManagerService piggyBankManagerService;
    private final GoalCompletionService goalCompletionService;
    private final SettingsActivityService settingsActivityService;

    private final AutoPaymentsCore autoPaymentsCore;

    @Transactional
    public void createAutomation(Long userId, Long piggyBankId, AutoPaymentsDto autoPaymentsDto) {
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        piggyBankSettings.setAutomationActive(autoPaymentsDto.isAutomationActive());

        if (piggyBankSettings.isAutomationActive()) {
            validatePercentage(autoPaymentsDto);
            piggyBankSettings.setAutomationPercentage(autoPaymentsDto.percentage());
        } else {
            piggyBankSettings.setAutomationPercentage(BigDecimal.ZERO);
        }

    }

    @Transactional
    public AutoPaymentsDto getAutomation(Long userId, Long piggyBankId) {
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new AutoPaymentsDto(
                piggyBankSettings.isAutomationActive(),
                piggyBankSettings.getAutomationPercentage()
        );
    }

    @Transactional
    public void saveAutoPaymentsPiggyBank(Long userId, Long piggyBankId, AutoPaymentsDto settings) {
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        validatePercentage(settings);

        piggyBankSettings.setAutomationActive(settings.isAutomationActive());
        piggyBankSettings.setAutomationPercentage(settings.isAutomationActive() ? settings.percentage() : BigDecimal.ZERO);
        if (piggyBankSettings.isAutomationActive()) {
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.ENABLED, SettingType.PIGGY_BANK_AUTO_PAYMENTS);
        } else {
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.DISABLED, SettingType.PIGGY_BANK_AUTO_PAYMENTS);
        }
    }

    @Transactional
    public void handleRevenuePiggyBankAutomation(Long userId, BigDecimal revenueAmount, PiggyBankAutomationMode mode) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        if (user.getPiggyBanks() == null || user.getPiggyBanks().isEmpty()) return;

        for (PiggyBank piggyBank : user.getPiggyBanks()) {

            PiggyBankSettings settings = piggyBank.getSettings();

            if (!settings.isAutomationActive()) continue;

            BigDecimal automationAmount = CalculatePercentage.calculateValueFromPercentage(revenueAmount, settings.getAutomationPercentage());
            autoPaymentsCore.process(userId, piggyBank, wallet, automationAmount, mode);

        }

        goalCompletionService.handleGoalCompletion(userId);
    }

    private void validatePercentage(AutoPaymentsDto autoPaymentsDto) {
        if (autoPaymentsDto.isAutomationActive() && autoPaymentsDto.percentage() == null) {
            throw new IllegalArgumentException("Percentage is required");
        }
    }

}
