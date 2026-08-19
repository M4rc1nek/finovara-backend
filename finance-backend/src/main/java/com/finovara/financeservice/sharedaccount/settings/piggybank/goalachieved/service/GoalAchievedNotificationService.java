package com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service;

import com.finovara.contracts.finance.event.sharedaccount.GoalAchievedNotificationEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.dto.GoalAchievedNotificationDto;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoalAchievedNotificationService {

    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final OutboxService outboxService;

    @Transactional
    public void saveGoalAchievedNotification(Long userId, GoalAchievedNotificationDto settings) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        sharedAccountSettings.setPiggyBankGoalAchievedNotificationEnabled(settings.piggyBankGoalAchievedNotificationEnabled());
    }

    @Transactional
    public GoalAchievedNotificationDto getGoalAchievedNotification(Long userId) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        return new GoalAchievedNotificationDto(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled());
    }

    @Transactional
    public void handleGoalAchieved(Long userId, SharedPiggyBank sharedPiggyBank) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);
        if (!sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()) return;

        if (sharedPiggyBank.isGoalAchievedNotified()) return;

        boolean isCompleted = PiggyBankCheckGoalCompletion.isSharedPiggyBankGoalCompleted(sharedPiggyBank);

        if (isCompleted) {
            sharedPiggyBank.setGoalAchievedNotified(true);

            outboxService.save("PiggyBank", userId.toString(), "notification.shared-account.piggy-bank-goal-achieved",
                    new GoalAchievedNotificationEvent(
                            sharedPiggyBank.getOwnerId(),
                            sharedPiggyBank.getMemberId(),
                            userId,
                            sharedPiggyBank.getId(),
                            sharedPiggyBank.getAmount(),
                            sharedPiggyBank.getGoalAmount(),
                            LocalDateTime.now()));
        }
    }
}
