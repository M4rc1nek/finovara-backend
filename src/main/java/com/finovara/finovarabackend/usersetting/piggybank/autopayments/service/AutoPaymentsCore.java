package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AutoPaymentsCore {
    private final PiggyBankActivityService piggyBankActivityService;

    public void process(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount, PiggyBankAutomationMode mode) {
        switch (mode) {
            case APPLY -> apply(userId, piggyBank, wallet, automationAmount);
            case ROLLBACK -> rollback(userId, piggyBank, wallet, automationAmount);
        }
    }

    private void apply(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount) {
        BigDecimal availableToTransfer = wallet.getBalance().min(automationAmount);

        piggyBank.setAmount(piggyBank.getAmount().add(availableToTransfer));
        wallet.setBalance(wallet.getBalance().subtract(availableToTransfer));

        piggyBankActivityService.createPaymentPiggyBankActivity(
                userId,
                piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING,
                availableToTransfer
        );
    }

    private void rollback(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount) {
        BigDecimal amountToRollback = automationAmount.min(piggyBank.getAmount());

        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollback));
        wallet.setBalance(wallet.getBalance().add(amountToRollback));

        piggyBankActivityService.createPaymentPiggyBankActivity(
                userId,
                piggyBank,
                PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING,
                amountToRollback
        );
    }
}
