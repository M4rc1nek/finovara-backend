package com.finovara.financeservice.settings.piggybank.roundup.service;

import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.financeservice.settings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.financeservice.util.transaction.expense.ExpenseManagerService;
import com.finovara.financeservice.util.transaction.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoundUpService {

    private final ExpenseManagerService expenseManagerService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final GoalCompletionService goalCompletionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RoundUpCore roundUpCore;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public RoundUpDto getRoundUp(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new RoundUpDto(piggyBankSettings.isRoundUpActive(), null);
    }

    @Transactional
    public void saveRoundUpPiggyBank(Long userId, Long piggyBankId, RoundUpDto dto) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));

        PiggyBankSettings settings = piggyBank.getSettings();
        settings.setRoundUpActive(dto.roundUpActive());
        if (settings.isRoundUpActive()) {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.PIGGY_BANK_ROUND_UP, SettingActivityStatus.ENABLED, LocalDateTime.now()));
        } else {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.PIGGY_BANK_ROUND_UP, SettingActivityStatus.DISABLED, LocalDateTime.now()));
        }
    }

    @Transactional
    public void handleExpenseForRoundUp(Long userId, Long expenseId, PiggyBankAutomationMode mode) {
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, userId);

        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserId(userId);

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(() -> new RequestedEntityNotFoundException("Wallet not found"));

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        BigDecimal expenseAmount = expense.getAmount();
        BigDecimal roundUpAmount = calculateRoundUp(expenseAmount);

        for (PiggyBank piggyBank : piggyBanks) {

            PiggyBankSettings settings = piggyBank.getSettings();

            if (!settings.isRoundUpActive()) continue;

            roundUpCore.process(userId, piggyBank, wallet, roundUpAmount, mode);
        }

        goalCompletionService.handleGoalCompletion(userId);
    }

    private BigDecimal calculateRoundUp(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.CEILING).subtract(amount);
    }

}
