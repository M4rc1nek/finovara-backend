package com.finovara.finovarabackend.usersetting.piggybank.completion.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GoalCompletionCore {

    private final PiggyBankActivityService piggyBankActivityService;

    public void apply(String email, PiggyBank piggyBank, Wallet wallet, User user, GoalCompletionStrategy strategy) {

        switch (strategy) {
            case NONE -> {
            }

            case WITHDRAW_AND_KEEP -> withdrawAndKeep(email, piggyBank, wallet);

            case WITHDRAW_AND_DELETE -> withdrawAndDelete(email, piggyBank, wallet, user);
        }
    }

    private void withdrawAndKeep(String email, PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amountToTransfer = piggyBank.getAmount();
        transferFunds(piggyBank, wallet);

        piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountToTransfer);
    }

    private void withdrawAndDelete(String email, PiggyBank piggyBank, Wallet wallet, User user) {
        BigDecimal amountToTransfer = piggyBank.getAmount();

        transferFunds(piggyBank, wallet);

        piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountToTransfer);
        piggyBankActivityService.createSimplePiggyBankActivity(email, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);

        if (piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance.");
        }

        user.getPiggyBanks().remove(piggyBank);
    }

    private void transferFunds(PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amount = piggyBank.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        piggyBank.setAmount(BigDecimal.ZERO);
    }
}