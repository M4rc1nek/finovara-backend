package com.finovara.notificationservice.notification.consumer.sharedaccount.deletion;

import com.finovara.contracts.event.notification.sharedaccount.deletion.NotificationSharedAccountLeftEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountLeftDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SharedAccountLeftConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.shared-account.left")
    public void handle(NotificationSharedAccountLeftEvent event) {
        notificationPersistenceService.save(event.recipientUserId(), new SharedAccountLeftDto(
                NotificationType.SHARED_ACCOUNT_LEFT,
                LocalDateTime.now(),
                event.leftUsername()
        ));

        log.info("Shared account left notification saved: recipientUserId={}, leftUsername={}",
                event.recipientUserId(), event.leftUsername());
    }
}