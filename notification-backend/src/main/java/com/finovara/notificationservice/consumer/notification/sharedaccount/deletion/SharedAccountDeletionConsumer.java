package com.finovara.notificationservice.consumer.notification.sharedaccount.deletion;

import com.finovara.contracts.mainaccount.notification.NotificationType;
import com.finovara.contracts.sharedaccount.event.deletion.NotificationSharedAccountDeletedEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountDeletedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SharedAccountDeletionConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.shared-account.deleted")
    public void handle(NotificationSharedAccountDeletedEvent event) {
        notificationPersistenceService.save(event.recipientUserId(), new SharedAccountDeletedDto(
                NotificationType.SHARED_ACCOUNT_DELETED,
                LocalDateTime.now(),
                event.deletedByUsername()
        ));

        log.info("Shared account deleted notification saved: recipientUserId={}, deletedByUsername={}",
                event.recipientUserId(), event.deletedByUsername());
    }
}