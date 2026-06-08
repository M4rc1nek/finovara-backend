package com.finovara.corebackend.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.corebackend.util.piggybank.PiggyBankCalculator;
import com.finovara.corebackend.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.corebackend.util.piggybank.PiggyBankValidator;
import com.finovara.corebackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.util.wallet.WalletManagerService;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PiggyBankTransactionService {
    private final WalletManagerService walletManagerService;
    private final UserManagerService userManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final GoalCompletionService goalCompletionService;

    @Transactional
    public void addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount, PiggyBankActivityType piggyBankActivityType) {
        UserContext ctx = getEntitiesForTransaction(userId, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        ctx.wallet.withdraw(amount);
        ctx.piggyBank.setAmount(ctx.piggyBank.getAmount().add(amount));

        BigDecimal percentage = calculatePercentage(ctx.piggyBank);
        boolean completed = PiggyBankCheckGoalCompletion.isGoalCompleted(ctx.piggyBank);

        walletRepository.save(ctx.wallet);
        piggyBankRepository.save(ctx.piggyBank);

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, piggyBankActivityType, ctx.piggyBank.getName(), ctx.piggyBank.getGoalType(), ctx.piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        kafkaTemplate.send("piggybank.calculate-progress", new PiggyBankProgressEvent(userId, piggyBankId, percentage, ctx.piggyBank.getGoalType(), ctx.piggyBank.getName()));

        if (completed) {
            goalCompletionService.handleGoalCompletion(userId);
        }
    }

    @Transactional
    public void removeBalanceFromPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        UserContext userContext = getEntitiesForTransaction(userId, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankValidator.validateSufficientFunds(userContext.piggyBank.getAmount(), amount);

        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().subtract(amount));
        userContext.wallet.deposit(amount);

        BigDecimal percentage = calculatePercentage(userContext.piggyBank);

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, userContext.piggyBank.getName(), userContext.piggyBank.getGoalType()
                , userContext.piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        kafkaTemplate.send("piggybank.calculate-progress", new PiggyBankProgressEvent(userId, piggyBankId, percentage, userContext.piggyBank.getGoalType(), userContext.piggyBank.getName()));
    }

    private BigDecimal calculatePercentage(PiggyBank piggyBank) {
        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private UserContext getEntitiesForTransaction(Long userId, Long piggyBankId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        return new UserContext(wallet, piggyBank, user);
    }

    private record UserContext(Wallet wallet, PiggyBank piggyBank, User user) {
    }
}