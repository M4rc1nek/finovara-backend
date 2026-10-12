package com.finovara.notificationservice.consumer.notification.sharedaccount.invitation;

import com.finovara.contracts.mainaccount.notification.NotificationType;
import com.finovara.contracts.sharedaccount.event.invitation.SharedAccountInvitationExpiredEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.SharedAccountInvitationExpiredDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SharedAccountInvitationExpiredConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.shared-account.invitation-expired")
    public void handle(SharedAccountInvitationExpiredEvent event) {

        notificationPersistenceService.save(event.userId(), new SharedAccountInvitationExpiredDto(
                NotificationType.SHARED_ACCOUNT_INVITATION_EXPIRED,
                LocalDateTime.now(),
                event.inviteeUsername()
        ));
        log.info("Shared account invitation expired: userId={}, inviteeUsername='{}'", event.userId(), event.inviteeUsername());
    }
}


