package com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.service;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.event.finance.sharedaccount.GoalAchievedNotificationEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.util.emailsender.EmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalAchievedNotificationHandler {

    private final AuthBackendClient authBackendClient;
    private final EmailNotifier emailNotifier;

    public void handle(GoalAchievedNotificationEvent event) {
        UserDataResponse triggeredByUser = authBackendClient.getUserEmailData(event.triggeredByUserId());
        String triggeredByUsername = triggeredByUser.username().orElse("Nieznany użytkownik");

        notifyRecipient(event.ownerId(), event, triggeredByUsername);
        notifyRecipient(event.memberId(), event, triggeredByUsername);

        log.info("Goal achieved notification processed piggyBankId={}, ownerId={}, memberId={}, triggeredByUserId={}, currentAmount={}, goalAmount={}",
                event.piggyBankId(), event.ownerId(), event.memberId(), event.triggeredByUserId(), event.currentAmount(), event.goalAmount());
    }

    private void notifyRecipient(Long recipientUserId, GoalAchievedNotificationEvent event, String triggeredByUsername) {
        UserDataResponse recipient = authBackendClient.getUserEmailData(recipientUserId);

        if (recipient.email().isEmpty()) {
            log.warn("Skipping goal achieved email - no email found for userId={}, piggyBankId={}", recipientUserId, event.piggyBankId());
            return;
        }

        emailNotifier.sendGoalAchieved(
                recipient.email().get(),
                recipient.username().orElse("Użytkowniku"),
                triggeredByUsername,
                event.currentAmount(),
                event.goalAmount(),
                event.occurredAt()
        );
    }
}