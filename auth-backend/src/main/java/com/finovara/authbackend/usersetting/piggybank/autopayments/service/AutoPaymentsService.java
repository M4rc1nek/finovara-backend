package com.finovara.authbackend.usersetting.piggybank.autopayments.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.authbackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.authbackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.authbackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.authbackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.authbackend.util.wallet.WalletManagerService;
import com.finovara.authbackend.wallet.model.Wallet;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPaymentsService {

    private final WalletManagerService walletManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final GoalCompletionService goalCompletionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AutoPaymentsCore autoPaymentsCore;

    @Transactional
    public void createAutomation(Long userId, Long piggyBankId, AutoPaymentsDto autoPaymentsDto) {
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
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new AutoPaymentsDto(piggyBankSettings.isAutomationActive(), piggyBankSettings.getAutomationPercentage());
    }

    @Transactional
    public void saveAutoPaymentsPiggyBank(Long userId, Long piggyBankId, AutoPaymentsDto settings) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        validatePercentage(settings);

        piggyBankSettings.setAutomationActive(settings.isAutomationActive());
        piggyBankSettings.setAutomationPercentage(settings.isAutomationActive() ? settings.percentage() : BigDecimal.ZERO);
        if (piggyBankSettings.isAutomationActive()) {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.PIGGY_BANK_AUTO_PAYMENTS, SettingActivityStatus.ENABLED, LocalDateTime.now()));
        } else {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.PIGGY_BANK_AUTO_PAYMENTS, SettingActivityStatus.DISABLED, LocalDateTime.now()));
        }
    }

    @Transactional
    public void handleRevenuePiggyBankAutomation(Long userId, BigDecimal revenueAmount, PiggyBankAutomationMode mode) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserId(userId);

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        for (PiggyBank piggyBank : piggyBanks) {

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
