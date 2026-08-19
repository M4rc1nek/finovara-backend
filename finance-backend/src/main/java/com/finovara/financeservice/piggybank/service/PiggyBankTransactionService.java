package com.finovara.financeservice.piggybank.service;

import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.notification.event.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.piggybank.goalplanner.service.GoalPlannerService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.util.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.financeservice.util.piggybank.PiggyBankValidator;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
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
    private final GoalPlannerService goalPlannerService;
    private final WalletService walletService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount, PiggyBankActivityType piggyBankActivityType, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));


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
        goalPlannerService.checkAndMarkGoalCompletion(piggyBank.getGoalPlanner());
    }

    @Transactional
    public void removeBalanceFromPiggyBank(Long userId, Long piggyBankId, BigDecimal amount, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

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
        goalPlannerService.checkAndMarkGoalCompletion(piggyBank.getGoalPlanner());
    }

    private BigDecimal calculatePercentage(PiggyBank piggyBank) {
        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}
