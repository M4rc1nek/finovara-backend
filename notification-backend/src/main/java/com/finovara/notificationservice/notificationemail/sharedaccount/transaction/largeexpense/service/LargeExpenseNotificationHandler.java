package com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.service;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.finance.event.sharedaccount.LargeExpenseNotificationEvent;
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

    private final AuthBackendClient authBackendClient;
    private final EmailNotifier emailNotifier;

    public void handle(LargeExpenseNotificationEvent event) {
        UserDataResponse triggeredByUser = authBackendClient.getUserEmailData(event.triggeredByUserId());
        String triggeredByUsername = triggeredByUser.username().orElse("Nieznany użytkownik");

        notifyRecipient(event.ownerId(), event, triggeredByUsername);
        notifyRecipient(event.memberId(), event, triggeredByUsername);

        log.info("Large expense notification processed expenseId={}, ownerId={}, memberId={}, triggeredByUserId={}, amount={}, threshold={}",
                event.expenseId(), event.ownerId(), event.memberId(), event.triggeredByUserId(), event.amount(), event.threshold());
    }

    private void notifyRecipient(Long recipientUserId, LargeExpenseNotificationEvent event, String triggeredByUsername) {
        UserDataResponse recipient = authBackendClient.getUserEmailData(recipientUserId);

        if (recipient.email().isEmpty()) {
            log.warn("Skipping large expense email - no email found for userId={}, expenseId={}", recipientUserId, event.expenseId());
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        emailNotifier.send(
                ActionEmailNotificationType.LARGE_EXPENSE_DETECTED,
                recipient.email().get(),
                Map.of(
                        "username", recipient.username().orElse("Użytkowniku"),
                        "triggeredByUsername", triggeredByUsername,
                        "amount", event.amount().toPlainString(),
                        "threshold", event.threshold().toPlainString(),
                        "occurredAt", event.occurredAt().format(formatter)
                )
        );
    }
}