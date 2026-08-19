package com.finovara.notificationservice.notification;

import com.finovara.contracts.user.event.account.delete.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "user-account.deleted", groupId = "notification-service")
    public void deleteAllNotifications(UserAccountDeletedEvent event) {
        notificationPersistenceService.deleteAllNotifications(event.userId());
    }
}
