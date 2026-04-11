package com.finovara.finovarabackend.usersetting.piggybank.roundup.service;

import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDto;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankManagementService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.util.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final PiggyBankActivityService piggyBankActivityService;
    private final GoalCompletionService goalCompletionService;
    private final SettingsActivityService settingsActivityService;
    private final RoundUpCore roundUpCore;

    @Transactional
    public RoundUpDto getRoundUp(String email, Long piggyBankId) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new RoundUpDto(piggyBankSettings.isRoundUpActive());
    }

    @Transactional
    public Long addDefaultPiggyBank(PiggyBankDto piggyBankDto, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        return piggyBankManagementService.addPiggyBank(piggyBankDto, user.getEmail());
    }

    @Transactional
    public void saveRoundUpPiggyBank(String email, Long piggyBankId, RoundUpDto dto) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);

        PiggyBankSettings settings = piggyBank.getSettings();
        settings.setRoundUpActive(dto.roundUpActive());
        if (settings.isRoundUpActive()) {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.PIGGY_BANK_ROUND_UP);
        } else {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.PIGGY_BANK_ROUND_UP);
        }
    }

    @Transactional
    public void handleExpenseForRoundUp(String email, Long expenseId, PiggyBankAutomationMode mode) {

        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());

        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedEmail(email);

        Wallet wallet = walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        BigDecimal expenseAmount = expense.getAmount();
        BigDecimal roundUpAmount = calculateRoundUp(expenseAmount);

        for (PiggyBank piggyBank : piggyBanks) {

            PiggyBankSettings settings = piggyBank.getSettings();

            if (!settings.isRoundUpActive()) continue;

            roundUpCore.process(email, piggyBank, wallet, roundUpAmount, mode);
        }

        goalCompletionService.handleGoalCompletion(email);
    }

    private BigDecimal calculateRoundUp(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.CEILING).subtract(amount);
    }

}
