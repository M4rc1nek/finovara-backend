package com.finovara.financeservice.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.util.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.financeservice.util.piggybank.PiggyBankValidator;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PiggyBankTransactionService {
    private final PiggyBankManagerService piggyBankManagerService;
    private final OutboxService outboxService;
    private final GoalCompletionService goalCompletionService;
    private final WalletService walletService;

    @Transactional
    public void addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount, PiggyBankActivityType piggyBankActivityType) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankValidator.validateAmount(amount);
        walletService.removeBalanceFromWallet(userId, amount);
        piggyBank.setAmount(piggyBank.getAmount().add(amount));

        BigDecimal percentage = calculatePercentage(piggyBank);
        boolean completed = PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank);

        outboxService.save("PiggyBank", piggyBankId.toString(), "activity.piggybank",
                new PiggyBankActivityEvent(userId, piggyBankActivityType, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        outboxService.save("PiggyBank", piggyBankId.toString(), "piggybank.calculate-progress",
                new PiggyBankProgressEvent(userId, piggyBankId, percentage, piggyBank.getGoalType(), piggyBank.getName()));

        if (completed) {
            goalCompletionService.handleGoalCompletion(userId);
        }
    }

    @Transactional
    public void removeBalanceFromPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankValidator.validateSufficientFunds(piggyBank.getAmount(), amount);

        piggyBank.setAmount(piggyBank.getAmount().subtract(amount));
        walletService.addBalanceToWallet(userId, amount);

        BigDecimal percentage = calculatePercentage(piggyBank);

        outboxService.save("PiggyBank", piggyBankId.toString(), "activity.piggybank",
                new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        outboxService.save("PiggyBank", piggyBankId.toString(), "piggybank.calculate-progress",
                new PiggyBankProgressEvent(userId, piggyBankId, percentage, piggyBank.getGoalType(), piggyBank.getName()));
    }

    private BigDecimal calculatePercentage(PiggyBank piggyBank) {
        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}
