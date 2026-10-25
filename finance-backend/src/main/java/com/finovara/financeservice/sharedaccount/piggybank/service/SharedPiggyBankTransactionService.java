package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service.GoalAchievedNotificationService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.piggybank.SharedPiggyBankManager;
import com.finovara.financeservice.util.sharedaccount.settings.SharedSettingsManager;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SharedPiggyBankTransactionService {

    private final SharedPiggyBankManager sharedPiggyBankManager;
    private final SharedWalletService sharedWalletService;
    private final GoalAchievedNotificationService goalAchievedNotificationService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final OutboxService outboxService;

    @Transactional
    public BigDecimal addBalanceToPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        PiggyBankValidator.validateAmount(amount);

        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);

        sharedWalletService.removeBalanceFromWallet(userId, amount);
        SharedPiggyBank piggyBank = sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(piggyBankId, userId);

        piggyBank.setAmount(piggyBank.getAmount().add(amount));

        logActivity(participants, userId, piggyBankId, SharedAccountActivityLogType.PIGGY_BANK_DEPOSIT);
        goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

        return calculatePercentage(piggyBank);
    }

    @Transactional
    public BigDecimal removeBalanceFromPiggyBank(Long userId, Long piggyBankId, BigDecimal amount) {
        PiggyBankValidator.validateAmount(amount);

        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);

        sharedWalletService.lockWalletForUpdate(userId);
        SharedPiggyBank piggyBank = sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(piggyBankId, userId);

        PiggyBankValidator.validateSufficientFunds(piggyBank.getAmount(), amount);
        piggyBank.setAmount(piggyBank.getAmount().subtract(amount));
        sharedWalletService.addBalanceToWallet(userId, amount);

        logActivity(participants, userId, piggyBankId, SharedAccountActivityLogType.PIGGY_BANK_WITHDRAWAL);

        if (piggyBank.getGoalAmount() != null && piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) < 0) {
            piggyBank.setGoalAchievedNotified(false);
        }

        return calculatePercentage(piggyBank);
    }

    private void logActivity(SharedAccountParticipantsResponse participants, Long userId, Long piggyBankId, SharedAccountActivityLogType type) {
        outboxService.save("SharedAccountPiggyBank", piggyBankId.toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, piggyBankId, type, LocalDateTime.now()));
    }

    private BigDecimal calculatePercentage(SharedPiggyBank sharedPiggyBank) {
        Double progress = PiggyBankCalculator.calculateSharedPiggyBankProgress(sharedPiggyBank);
        return BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}