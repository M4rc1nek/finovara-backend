package com.finovara.finovarabackend.usersetting.piggybank.completion.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GoalCompletionCore {

    private final PiggyBankActivityService piggyBankActivityService;
    private final RecurringSettingsRepository recurringSettingsRepository;

    public void apply(Long userId, PiggyBank piggyBank, Wallet wallet, User user, GoalCompletionStrategy strategy) {

        switch (strategy) {
            case NONE -> {
            }

            case WITHDRAW_AND_KEEP -> withdrawAndKeep(userId, piggyBank, wallet);

            case WITHDRAW_AND_DELETE -> withdrawAndDelete(userId, piggyBank, wallet, user);
        }
    }

    private void withdrawAndKeep(Long userId, PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amountToTransfer = piggyBank.getAmount();
        transferFunds(piggyBank, wallet);

        piggyBankActivityService.createPaymentPiggyBankActivity(userId, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountToTransfer);
    }

    private void withdrawAndDelete(Long userId, PiggyBank piggyBank, Wallet wallet, User user) {
        BigDecimal amountToTransfer = piggyBank.getAmount();

        transferFunds(piggyBank, wallet);

        piggyBankActivityService.createPaymentPiggyBankActivity(userId, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountToTransfer);
        piggyBankActivityService.createSimplePiggyBankActivity(userId, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);

        recurringSettingsRepository.findByUserAssignedIdAndPiggyBankId(user.getId(), piggyBank.getId())
                .ifPresent(settings -> {
                    settings.setEnable(false);
                    settings.setPiggyBankId(null);
                    settings.setNextExecutionDate(null);
                });

        user.getPiggyBanks().remove(piggyBank);
    }

    private void transferFunds(PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amount = piggyBank.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        wallet.deposit(amount);
        piggyBank.setAmount(BigDecimal.ZERO);
    }
}
