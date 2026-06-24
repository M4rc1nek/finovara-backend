package com.finovara.financeservice.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.util.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.financeservice.util.piggybank.PiggyBankValidator;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PiggyBankTransactionService {
    private final WalletManagerService walletManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final OutboxService outboxService;
    private final GoalCompletionService goalCompletionService;

    @Transactional
    public void addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount, PiggyBankActivityType piggyBankActivityType) {
        TransactionContext transactionContext = getEntitiesForTransaction(userId, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        transactionContext.wallet.withdraw(amount);
        transactionContext.piggyBank.setAmount(transactionContext.piggyBank.getAmount().add(amount));

        BigDecimal percentage = calculatePercentage(transactionContext.piggyBank);
        boolean completed = PiggyBankCheckGoalCompletion.isGoalCompleted(transactionContext.piggyBank);

        walletRepository.save(transactionContext.wallet);
        piggyBankRepository.save(transactionContext.piggyBank);

        outboxService.save("PiggyBank", piggyBankId.toString(), "activity.piggybank",
                new PiggyBankActivityEvent(userId, piggyBankActivityType, transactionContext.piggyBank.getName(), transactionContext.piggyBank.getGoalType(), transactionContext.piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        outboxService.save("PiggyBank", piggyBankId.toString(), "piggybank.calculate-progress",
                new PiggyBankProgressEvent(userId, piggyBankId, percentage, transactionContext.piggyBank.getGoalType(), transactionContext.piggyBank.getName()));

        if (completed) {
            goalCompletionService.handleGoalCompletion(userId);
        }
    }

    @Transactional
    public void removeBalanceFromPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        TransactionContext transactionContext = getEntitiesForTransaction(userId, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankValidator.validateSufficientFunds(transactionContext.piggyBank.getAmount(), amount);

        transactionContext.piggyBank.setAmount(transactionContext.piggyBank.getAmount().subtract(amount));
        transactionContext.wallet.deposit(amount);

        BigDecimal percentage = calculatePercentage(transactionContext.piggyBank);

        walletRepository.save(transactionContext.wallet);
        piggyBankRepository.save(transactionContext.piggyBank);

        outboxService.save("PiggyBank", piggyBankId.toString(), "activity.piggybank",
                new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, transactionContext.piggyBank.getName(), transactionContext.piggyBank.getGoalType(), transactionContext.piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        outboxService.save("PiggyBank", piggyBankId.toString(), "piggybank.calculate-progress",
                new PiggyBankProgressEvent(userId, piggyBankId, percentage, transactionContext.piggyBank.getGoalType(), transactionContext.piggyBank.getName()));
    }

    private BigDecimal calculatePercentage(PiggyBank piggyBank) {
        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private TransactionContext getEntitiesForTransaction(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        return new TransactionContext(wallet, piggyBank);
    }

    private record TransactionContext(Wallet wallet, PiggyBank piggyBank) {
    }
}
