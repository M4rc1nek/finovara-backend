package com.finovara.corebackend.usersetting.piggybank.roundup.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.piggybank.dto.PiggyBankDto;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
import com.finovara.corebackend.piggybank.service.PiggyBankManagementService;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.corebackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.corebackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.corebackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.corebackend.util.expense.ExpenseManagerService;
import com.finovara.corebackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoundUpService {

    private final UserManagerService userManagerService;
    private final ExpenseManagerService expenseManagerService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankManagementService piggyBankManagementService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final GoalCompletionService goalCompletionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RoundUpCore roundUpCore;

    @Transactional
    public RoundUpDto getRoundUp(Long userId, Long piggyBankId) {
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new RoundUpDto(piggyBankSettings.isRoundUpActive());
    }

    @Transactional
    public Long addDefaultPiggyBank(PiggyBankDto piggyBankDto, Long userId) {
        userManagerService.getUserByIdOrThrow(userId);
        return piggyBankManagementService.addPiggyBank(piggyBankDto, userId);
    }

    @Transactional
    public void saveRoundUpPiggyBank(Long userId, Long piggyBankId, RoundUpDto dto) {
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

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

        User user = userManagerService.getUserByIdOrThrow(userId);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());

        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedId(userId);

        Wallet wallet = walletRepository.findByUserAssignedId(userId).orElseThrow(() -> new RequestedEntityNotFoundException("Wallet not found"));

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
