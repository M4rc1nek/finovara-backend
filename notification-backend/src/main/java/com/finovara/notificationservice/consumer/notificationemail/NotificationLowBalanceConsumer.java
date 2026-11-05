package com.finovara.notificationservice.consumer.notificationemail;

import com.finovara.contracts.mainaccount.transaction.event.wallet.WalletBalanceChangedEvent;
import com.finovara.notificationservice.notificationemail.service.wallet.lowbalance.LowBalanceNotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationLowBalanceConsumer {

    private final LowBalanceNotificationHandler lowBalanceNotificationHandler;

    @KafkaListener(topics = "wallet.balance-changed", groupId = "notification-email-service")
    public void consume(WalletBalanceChangedEvent event) {
        lowBalanceNotificationHandler.handle(event);
    }
}
