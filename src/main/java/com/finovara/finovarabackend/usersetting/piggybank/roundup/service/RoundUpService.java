package com.finovara.finovarabackend.usersetting.piggybank.roundup.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankManagementService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
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

    @Transactional
    public RoundUpDto getRoundUp(String email, Long piggyBankId) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new RoundUpDto(piggyBankSettings.isRoundUpActive());
    }

    @Transactional
    public Long addDefaultPiggyBank(PiggyBankDTO piggyBankDTO, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        return piggyBankManagementService.addPiggyBank(piggyBankDTO, user.getEmail());
    }

    @Transactional
    public void saveRoundUpPiggyBank(String email, Long piggyBankId, RoundUpDto dto) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);

        PiggyBankSettings settings = piggyBank.getSettings();
        settings.setRoundUpActive(dto.roundUpActive());
        if(settings.isRoundUpActive()){
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.PIGGY_BANK_ROUND_UP);
        }else {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.PIGGY_BANK_ROUND_UP);
        }
    }

    @Transactional
    public void handleExpenseForRoundUp(String email, Long expenseId, AutoPaymentsMode mode) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedEmail(email);
        Wallet wallet = walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        for (PiggyBank piggyBank : piggyBanks) {
            PiggyBankSettings piggyBankSettings = piggyBank.getSettings();
            if (piggyBankSettings.isRoundUpActive()) {
                BigDecimal expenseAmount = expense.getAmount();
                BigDecimal roundUpAmount = expenseAmount.setScale(0, RoundingMode.CEILING).subtract(expenseAmount);
                if (wallet.getBalance().compareTo(roundUpAmount) < 0) {
                    throw new InvalidInputException("Insufficient funds for round-up");
                }
                switch (mode) {
                    case APPLY -> {
                        if (roundUpAmount.compareTo(BigDecimal.ZERO) > 0) {
                            piggyBank.setAmount(piggyBank.getAmount().add(roundUpAmount));
                            wallet.setBalance(wallet.getBalance().subtract(roundUpAmount));
                            piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, roundUpAmount);
                        }
                    }
                    case ROLLBACK -> {
                        BigDecimal amountToRollBack = roundUpAmount.min(piggyBank.getAmount());
                        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollBack));
                        wallet.setBalance(wallet.getBalance().add(amountToRollBack));
                        piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, roundUpAmount);
                    }
                }
            }
        }
        goalCompletionService.handleGoalCompletion(email);
    }

}
