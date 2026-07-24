package com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.consumer;

import com.finovara.contracts.event.finance.sharedaccount.LargeExpenseNotificationEvent;
import com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.service.LargeExpenseNotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LargeExpenseNotificationConsumer {

    private final LargeExpenseNotificationHandler largeExpenseNotificationHandler;

    @KafkaListener(topics = "notification.shared-account.large-expense-detected", groupId = "notification-email-service")
    public void consume(LargeExpenseNotificationEvent event) {
        largeExpenseNotificationHandler.handle(event);
    }
}