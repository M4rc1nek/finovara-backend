package com.finovara.authbackend.usersetting.piggybank.completion.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.authbackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.authbackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoalCompletionCore {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RecurringSettingsRepository recurringSettingsRepository;
    private final PiggyBankRepository piggyBankRepository;

    public void apply(Long userId, PiggyBank piggyBank, Wallet wallet, GoalCompletionStrategy strategy) {

        switch (strategy) {
            case NONE -> {
            }

            case WITHDRAW_AND_KEEP -> withdrawAndKeep(userId, piggyBank, wallet);

            case WITHDRAW_AND_DELETE -> withdrawAndDelete(userId, piggyBank, wallet);
        }
    }

    private void withdrawAndKeep(Long userId, PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amountToTransfer = piggyBank.getAmount();
        transferFunds(piggyBank, wallet);

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), amountToTransfer, LocalDateTime.now()));
    }

    private void withdrawAndDelete(Long userId, PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amountToTransfer = piggyBank.getAmount();

        transferFunds(piggyBank, wallet);

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), amountToTransfer, java.time.LocalDateTime.now()));
        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.DELETED_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), null, LocalDateTime.now()));

        recurringSettingsRepository.findByUserIdAndPiggyBankId(userId, piggyBank.getId()).ifPresent(settings -> {
            settings.setEnable(false);
            settings.setPiggyBankId(null);
            settings.setNextExecutionDate(null);
        });

        piggyBankRepository.delete(piggyBank);
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
