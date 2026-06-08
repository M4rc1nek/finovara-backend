package com.finovara.notificationservice.kafka;

import com.finovara.contracts.event.user.UserAccountDeletedEvent;
import com.finovara.notificationservice.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "user-account.deleted")
    public void deleteAllNotifications(UserAccountDeletedEvent event) {
        notificationRepository.deleteByUserId(event.userId());
    }
}
