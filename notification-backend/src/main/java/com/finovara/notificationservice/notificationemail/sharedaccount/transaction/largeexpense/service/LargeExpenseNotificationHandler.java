package com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.service;

import com.finovara.contracts.sharedaccount.event.notification.LargeExpenseNotificationEvent;
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
public class LargeExpenseNotificationHandler {

    private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AuthBackendClient authBackendClient;
    private final EmailNotifier emailNotifier;

    public void handle(LargeExpenseNotificationEvent event) {
        UserDataResponse owner = authBackendClient.getUserData(event.ownerId());
        UserDataResponse member = authBackendClient.getUserData(event.memberId());

        UserDataResponse triggeredByUser = event.triggeredByUserId().equals(event.ownerId()) ? owner : member;
        String triggeredByUsername = triggeredByUser.username().orElse("Nieznany użytkownik");

        notifyRecipient(event.ownerId(), owner, event, triggeredByUsername);
        notifyRecipient(event.memberId(), member, event, triggeredByUsername);

        log.info("Large expense notification processed expenseId={}, ownerId={}, memberId={}, triggeredByUserId={}, amount={}, threshold={}",
                event.expenseId(), event.ownerId(), event.memberId(), event.triggeredByUserId(), event.amount(), event.threshold());
    }

    private void notifyRecipient(Long recipientUserId, UserDataResponse recipient,
                                 LargeExpenseNotificationEvent event, String triggeredByUsername) {
        if (recipient.email().isEmpty()) {
            log.warn("Skipping large expense email - no email found for userId={}, expenseId={}", recipientUserId, event.expenseId());
            return;
        }

        emailNotifier.send(
                ActionEmailNotificationType.SHARED_ACCOUNT_LARGE_EXPENSE_DETECTED,
                recipient.email().get(),
                Map.of(
                        "username", recipient.username().orElse("Użytkowniku"),
                        "triggeredByUsername", triggeredByUsername,
                        "amount", event.amount().toPlainString(),
                        "threshold", event.threshold().toPlainString(),
                        "occurredAt", event.occurredAt().format(OCCURRED_AT_FORMATTER)
                )
        );
    }
}