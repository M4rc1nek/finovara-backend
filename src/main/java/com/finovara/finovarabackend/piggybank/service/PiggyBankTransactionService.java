package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.finovarabackend.util.piggybank.PiggyBankValidator;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCalculator;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PiggyBankTransactionService {
    private final WalletManagerService walletManagerService;
    private final UserManagerService userManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankActivityService piggyBankActivityService;
    private final GoalCompletionService goalCompletionService;

    @Transactional
    public void addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount, PiggyBankActivityType piggyBankActivityType) {

        UserContext userContext = getEntitiesForTransaction(userId, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankValidator.validateSufficientFunds(userContext.wallet.getBalance(), amount);

        userContext.wallet.setBalance(userContext.wallet.getBalance().subtract(amount));
        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().add(amount));

        piggyBankActivityService.createPaymentPiggyBankActivity(userId, userContext.piggyBank,
                piggyBankActivityType, amount);
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
        userContext.wallet.setBalance(userContext.wallet.getBalance().add(amount));

        PiggyBankCalculator.calculateProgress(userContext.piggyBank);
        PiggyBankCheckGoalCompletion.isGoalCompleted((userContext.piggyBank));

        piggyBankActivityService.createPaymentPiggyBankActivity(userId, userContext.piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, amount);

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
