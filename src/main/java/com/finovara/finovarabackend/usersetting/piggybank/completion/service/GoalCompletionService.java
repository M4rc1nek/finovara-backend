package com.finovara.finovarabackend.usersetting.piggybank.completion.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalCompletionService {
    private final UserManagerService userManagerService;
    private final WalletManagerService walletManagerService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final PiggyBankActivityService piggyBankActivityService;

    @Transactional
    public void addGoalCompletion(Long piggyBankId, String email, GoalCompletionDto goalCompletionDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, user.getEmail());
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        if (piggyBank.getGoalAmount() == null || piggyBank.getGoalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Cannot set goal completion strategy for a piggy bank without a goal.");
        }

        piggyBankSettings.setGoalCompletionStrategy(goalCompletionDto.strategy());
        piggyBankRepository.save(piggyBank);

    }

    @Transactional
    public void saveGoalCompletion(String email, Long piggyBankId, GoalCompletionDto settings) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        piggyBankSettings.setGoalCompletionStrategy(settings.strategy());
    }

    @Transactional
    public GoalCompletionDto getCompletionDto(String email, Long piggyBankId) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();
        return new GoalCompletionDto(piggyBankSettings.getGoalCompletionStrategy());
    }

    @Transactional
    public void handleGoalCompletion(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Wallet wallet = walletManagerService.getWalletByUserEmailOrThrow(user.getEmail());
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedEmail(user.getEmail());

        for (PiggyBank piggyBank : piggyBanks) {
            PiggyBankSettings piggyBankSettings = piggyBank.getSettings();
            if (!PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)) {
                continue;
            }

            switch (piggyBankSettings.getGoalCompletionStrategy()) {
                case NONE -> {
                }

                case WITHDRAW_AND_KEEP -> {
                    BigDecimal amountToWithdraw = piggyBank.getAmount();
                    transferFund(piggyBank, wallet);
                    BigDecimal amountPaid = amountToWithdraw;
                    piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountPaid);
                }

                case WITHDRAW_AND_DELETE -> {
                    BigDecimal amountToWithdraw = piggyBank.getAmount();
                    transferFund(piggyBank, wallet);
                    BigDecimal amountPaid = amountToWithdraw;
                    piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountPaid);
                    piggyBankActivityService.createSimplePiggyBankActivity(email, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);
                    // Safety check: ensure no money is left before deleting.
                    if (piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        throw new InvalidInputException("Cannot delete piggy bank with balance.");
                    }
                    user.getPiggyBanks().remove(piggyBank); // User entity has orphanRemoval = true
                }
            }
        }
        walletRepository.save(wallet);
    }

    private void transferFund(PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amountToTransfer = piggyBank.getAmount();

        if (amountToTransfer == null || amountToTransfer.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        wallet.setBalance(wallet.getBalance().add(amountToTransfer));
        piggyBank.setAmount(BigDecimal.ZERO);
    }

}