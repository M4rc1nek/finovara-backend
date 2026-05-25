package com.finovara.corebackend.piggybank.service;

import com.finovara.activityservice.contracts.event.piggybank.PiggyBankActivityEvent;
import com.finovara.activityservice.contracts.model.activity.PiggyBankActivityType;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        UserContext userContext = getEntitiesForTransaction(userId, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        userContext.wallet.withdraw(amount);
        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().add(amount));

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, piggyBankActivityType, userContext.piggyBank.getName(), userContext.piggyBank.getGoalType(), userContext.piggyBank.getGoalAmount(), amount, LocalDateTime.now()));
        PiggyBankCalculator.calculateProgress(userContext.piggyBank);
        boolean completed = PiggyBankCheckGoalCompletion.isGoalCompleted((userContext.piggyBank));

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);

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

        PiggyBankCalculator.calculateProgress(userContext.piggyBank);
        PiggyBankCheckGoalCompletion.isGoalCompleted((userContext.piggyBank));

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, userContext.piggyBank.getName(), userContext.piggyBank.getGoalType(), userContext.piggyBank.getGoalAmount(), amount, LocalDateTime.now()));

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);
    }

    private record UserContext(Wallet wallet, PiggyBank piggyBank, User user) {

    }

    private UserContext getEntitiesForTransaction(Long userId, Long piggyBankId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        return new UserContext(wallet, piggyBank, user);
    }
}
