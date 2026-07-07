package com.finovara.notificationservice.notification.consumer.sharedaccount.invitation;

import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserAcceptSharedAccountInvitationDto;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAcceptSharedAccountInvitationConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.shared-account.invitation-accepted", groupId = "notification-shared-account")
    public void handle(UserAcceptSharedAccountInvitationEvent event) {

        notificationPersistenceService.save(event.userId(), new UserAcceptSharedAccountInvitationDto(
                NotificationType.USER_ACCEPT_SHARED_ACCOUNT_INVITATION,
                LocalDateTime.now(),
                event.inviteeUsername()
        ));
        log.info("Shared account invitation accepted: userId={}, inviteeUsername='{}'", event.userId(), event.inviteeUsername());
    }
}

