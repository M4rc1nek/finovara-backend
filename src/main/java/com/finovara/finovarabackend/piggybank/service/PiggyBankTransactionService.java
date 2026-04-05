package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankValidator;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankCalculator;
import com.finovara.finovarabackend.util.service.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
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
    public void addBalanceToPiggyBank(String email, Long piggyBankId, BigDecimal amount) {

        UserContext userContext = getEntitiesForTransaction(email, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankCalculator.validateSufficientFunds(userContext.wallet.getBalance(), amount);

        userContext.wallet.setBalance(userContext.wallet.getBalance().subtract(amount));
        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().add(amount));

        piggyBankActivityService.createPaymentPiggyBankActivity(email, userContext.piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, amount);
        PiggyBankCalculator.calculateProgress(userContext.piggyBank);
        boolean completed = PiggyBankCheckGoalCompletion.isGoalCompleted((userContext.piggyBank));

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);

        if (completed) {
            goalCompletionService.handleGoalCompletion(email);
        }
    }

    @Transactional
    public void removeBalanceFromPiggyBank(String email, Long piggyBankId, BigDecimal amount) {

        UserContext userContext = getEntitiesForTransaction(email, piggyBankId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankCalculator.validateSufficientFunds(userContext.piggyBank.getAmount(), amount);

        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().subtract(amount));
        userContext.wallet.setBalance(userContext.wallet.getBalance().add(amount));

        PiggyBankCalculator.calculateProgress(userContext.piggyBank);
        PiggyBankCheckGoalCompletion.isGoalCompleted((userContext.piggyBank));

        piggyBankActivityService.createPaymentPiggyBankActivity(email, userContext.piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, amount);

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);
    }

    private record UserContext(Wallet wallet, PiggyBank piggyBank, User user) {

    }

    private UserContext getEntitiesForTransaction(String email, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        Wallet wallet = walletManagerService.getWalletByUserEmailOrThrow(email);

        return new UserContext(wallet, piggyBank, user);
    }
}
