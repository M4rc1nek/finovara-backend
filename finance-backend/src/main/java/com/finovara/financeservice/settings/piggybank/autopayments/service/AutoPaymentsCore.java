package com.finovara.financeservice.settings.piggybank.autopayments.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutoPaymentsCore {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void process(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount, PiggyBankAutomationMode mode) {
        switch (mode) {
            case APPLY -> apply(userId, piggyBank, wallet, automationAmount);
            case ROLLBACK -> rollback(userId, piggyBank, wallet, automationAmount);
        }
    }

    private void apply(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount) {
        BigDecimal availableToTransfer = wallet.getBalance().min(automationAmount);

        piggyBank.setAmount(piggyBank.getAmount().add(availableToTransfer));
        if (availableToTransfer.signum() > 0) {
            wallet.withdraw(availableToTransfer);
        }

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), availableToTransfer, LocalDateTime.now()));
    }

    private void rollback(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal automationAmount) {
        BigDecimal amountToRollback = automationAmount.min(piggyBank.getAmount());

        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollback));
        if (amountToRollback.signum() > 0) {
            wallet.deposit(amountToRollback);
        }

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), amountToRollback, LocalDateTime.now()));
    }
}
