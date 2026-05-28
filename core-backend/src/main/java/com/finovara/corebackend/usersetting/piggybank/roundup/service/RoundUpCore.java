package com.finovara.corebackend.usersetting.piggybank.roundup.service;

import com.finovara.contracts.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.corebackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoundUpCore {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void process(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal roundUpAmount, PiggyBankAutomationMode mode) {

        switch (mode) {
            case APPLY -> apply(userId, piggyBank, wallet, roundUpAmount);
            case ROLLBACK -> rollback(userId, piggyBank, wallet, roundUpAmount);
        }
    }

    private void apply(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal roundUpAmount) {

        if (roundUpAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        piggyBank.setAmount(piggyBank.getAmount().add(roundUpAmount));
        wallet.withdraw(roundUpAmount);

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), roundUpAmount, LocalDateTime.now()));
    }

    private void rollback(Long userId, PiggyBank piggyBank, Wallet wallet, BigDecimal roundUpAmount) {

        BigDecimal amountToRollback = roundUpAmount.min(piggyBank.getAmount());

        if (amountToRollback.compareTo(BigDecimal.ZERO) <= 0) return;

        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollback));
        wallet.deposit(amountToRollback);

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), amountToRollback, LocalDateTime.now()));
    }
}