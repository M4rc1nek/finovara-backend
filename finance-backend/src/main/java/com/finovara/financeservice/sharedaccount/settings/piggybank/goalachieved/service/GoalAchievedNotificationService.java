package com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service;

import com.finovara.contracts.sharedaccount.event.notification.GoalAchievedNotificationEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.dto.GoalAchievedNotificationDto;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankCheckGoalCompletion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoalAchievedNotificationService {

    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final OutboxService outboxService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;

    @Transactional
    public void saveGoalAchievedNotification(Long userId, GoalAchievedNotificationDto settings) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        boolean changed = sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled() != settings.piggyBankGoalAchievedNotificationEnabled();

        if (changed) {
            sharedAccountSettings.setPiggyBankGoalAchievedNotificationEnabled(settings.piggyBankGoalAchievedNotificationEnabled());

            SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
            outboxService.save("SharedAccountSettings", userId.toString(), "shared-account.activity",
                    new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, null, SharedAccountActivityLogType.SETTING_CHANGED, LocalDateTime.now()));
        }
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
