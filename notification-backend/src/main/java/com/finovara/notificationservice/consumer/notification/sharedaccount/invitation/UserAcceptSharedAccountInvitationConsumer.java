package com.finovara.notificationservice.consumer.notification.sharedaccount.invitation;

import com.finovara.contracts.mainaccount.notification.NotificationType;
import com.finovara.contracts.sharedaccount.event.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserAcceptSharedAccountInvitationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserAcceptSharedAccountInvitationConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.shared-account.invitation-accepted")
    public void handle(UserAcceptSharedAccountInvitationEvent event) {

        notificationPersistenceService.save(event.userId(), new UserAcceptSharedAccountInvitationDto(
                NotificationType.USER_ACCEPT_SHARED_ACCOUNT_INVITATION,
                LocalDateTime.now(),
                event.inviteeUsername()
        ));
        log.info("Shared account invitation accepted: userId={}, inviteeUsername='{}'", event.userId(), event.inviteeUsername());
    }
}

