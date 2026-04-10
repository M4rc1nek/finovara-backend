package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AutoPaymentsCore {
    private final PiggyBankActivityService piggyBankActivityService;

    public void apply(String email, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount) {
        BigDecimal availableToTransfer = wallet.getBalance().min(automationAmount);

        piggyBank.setAmount(piggyBank.getAmount().add(availableToTransfer));
        wallet.setBalance(wallet.getBalance().subtract(availableToTransfer));

        piggyBankActivityService.createPaymentPiggyBankActivity(
                email,
                piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING,
                availableToTransfer
        );
    }

    public void rollback(String email, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount) {
        BigDecimal amountToRollback = automationAmount.min(piggyBank.getAmount());

        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollback));
        wallet.setBalance(wallet.getBalance().add(amountToRollback));

        piggyBankActivityService.createPaymentPiggyBankActivity(
                email,
                piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING,
                amountToRollback
        );
    }
}
