package com.finovara.notificationservice.notification.consumer.sharedaccount.deletion;

import com.finovara.contracts.event.notification.sharedaccount.deletion.NotificationSharedAccountDeletedEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountDeletedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
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