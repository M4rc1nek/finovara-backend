package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service.GoalAchievedNotificationService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankValidator;
import com.finovara.financeservice.util.transaction.piggybank.manager.SharedPiggyBankManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class SharedPiggyBankTransactionService {

    private final SharedPiggyBankManager sharedPiggyBankManager;
    private final SharedWalletService sharedWalletService;
    private final GoalAchievedNotificationService goalAchievedNotificationService;

    @Transactional
    public BigDecimal addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        SharedPiggyBank piggyBank = sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankValidator.validateAmount(amount);
        sharedWalletService.removeBalanceFromWallet(userId, amount);
        piggyBank.setAmount(piggyBank.getAmount().add(amount));

        goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

        return calculatePercentage(piggyBank);
    }

    @Transactional
    public BigDecimal removeBalanceFromPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        SharedPiggyBank piggyBank = sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId);

        PiggyBankValidator.validateAmount(amount);
        PiggyBankValidator.validateSufficientFunds(piggyBank.getAmount(), amount);

        piggyBank.setAmount(piggyBank.getAmount().subtract(amount));
        sharedWalletService.addBalanceToWallet(userId, amount);

        if (piggyBank.getGoalAmount() != null && piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) < 0) {
            piggyBank.setGoalAchievedNotified(false);
        }

        return calculatePercentage(piggyBank);
    }

    private BigDecimal calculatePercentage(SharedPiggyBank sharedPiggyBank) {
        Double progress = PiggyBankCalculator.calculateSharedPiggyBankProgress(sharedPiggyBank);
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}