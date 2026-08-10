package com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.service;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.event.finance.sharedaccount.LargeExpenseNotificationEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.util.emailsender.EmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        emailNotifier.sendLargeExpenseDetected(
                recipient.email().get(),
                recipient.username().orElse("Użytkowniku"),
                triggeredByUsername,
                event.amount(),
                event.threshold(),
                event.occurredAt()
        );
    }
}