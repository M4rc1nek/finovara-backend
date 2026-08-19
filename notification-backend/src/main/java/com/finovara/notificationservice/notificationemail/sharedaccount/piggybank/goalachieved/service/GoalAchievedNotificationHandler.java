package com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.service;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.finance.event.sharedaccount.GoalAchievedNotificationEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        emailNotifier.send(
                ActionEmailNotificationType.PIGGY_BANK_GOAL_ACHIEVED,
                recipient.email().get(),
                Map.of(
                        "username", recipient.username().orElse("Użytkowniku"),
                        "triggeredByUsername", triggeredByUsername,
                        "currentAmount", event.currentAmount().toPlainString(),
                        "goalAmount", event.goalAmount().toPlainString(),
                        "occurredAt", event.occurredAt().format(formatter)
                )
        );
    }
}