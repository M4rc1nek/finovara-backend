package com.finovara.finovarabackend.usersetting.piggybank.roundup.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RoundUpCore {

    private final PiggyBankActivityService piggyBankActivityService;

    public void process(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal roundUpAmount, PiggyBankAutomationMode mode) {

        switch (mode) {
            case APPLY -> apply(userId, piggyBank, wallet, roundUpAmount);
            case ROLLBACK -> rollback(userId, piggyBank, wallet, roundUpAmount);
        }
    }

    private void apply(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal roundUpAmount) {

        if (roundUpAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        if (wallet.getBalance().compareTo(roundUpAmount) < 0) {
            throw new InvalidInputException("Insufficient funds for round-up");
        }

        piggyBank.setAmount(piggyBank.getAmount().add(roundUpAmount));
        wallet.setBalance(wallet.getBalance().subtract(roundUpAmount));

        piggyBankActivityService.createPaymentPiggyBankActivity(userId, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, roundUpAmount);
    }

    private void rollback(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal roundUpAmount) {

        BigDecimal amountToRollback = roundUpAmount.min(piggyBank.getAmount());

        if (amountToRollback.compareTo(BigDecimal.ZERO) <= 0) return;

        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollback));
        wallet.setBalance(wallet.getBalance().add(amountToRollback));

        piggyBankActivityService.createPaymentPiggyBankActivity(userId, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, amountToRollback);
    }
}