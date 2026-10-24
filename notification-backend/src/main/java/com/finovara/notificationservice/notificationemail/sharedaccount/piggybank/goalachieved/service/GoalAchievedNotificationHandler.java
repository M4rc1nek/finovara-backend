package com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.service;

import com.finovara.contracts.sharedaccount.event.notification.GoalAchievedNotificationEvent;
import com.finovara.contracts.user.authorization.dto.UserDataResponse;
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

    private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AuthBackendClient authBackendClient;
    private final EmailNotifier emailNotifier;

    public void handle(GoalAchievedNotificationEvent event) {
        UserDataResponse owner = authBackendClient.getUserData(event.ownerId());
        UserDataResponse member = authBackendClient.getUserData(event.memberId());

        UserDataResponse triggeredByUser = event.triggeredByUserId().equals(event.ownerId()) ? owner : member;
        String triggeredByUsername = triggeredByUser.username().orElse("Nieznany użytkownik");

        notifyRecipient(event.ownerId(), owner, event, triggeredByUsername);
        notifyRecipient(event.memberId(), member, event, triggeredByUsername);

        log.info("Goal achieved notification processed piggyBankId={}, ownerId={}, memberId={}, triggeredByUserId={}, currentAmount={}, goalAmount={}",
                event.piggyBankId(), event.ownerId(), event.memberId(), event.triggeredByUserId(), event.currentAmount(), event.goalAmount());
    }

    private void notifyRecipient(Long recipientUserId, UserDataResponse recipient,
                                 GoalAchievedNotificationEvent event, String triggeredByUsername) {
        if (recipient.email().isEmpty()) {
            log.warn("Skipping goal achieved email - no email found for userId={}, piggyBankId={}", recipientUserId, event.piggyBankId());
            return;
        }

        emailNotifier.send(
                ActionEmailNotificationType.SHARED_ACCOUNT_PIGGY_BANK_GOAL_ACHIEVED,
                recipient.email().get(),
                Map.of(
                        "username", recipient.username().orElse("Użytkowniku"),
                        "triggeredByUsername", triggeredByUsername,
                        "currentAmount", event.currentAmount().toPlainString(),
                        "goalAmount", event.goalAmount().toPlainString(),
                        "occurredAt", event.occurredAt().format(OCCURRED_AT_FORMATTER)
                )
        );
    }
}