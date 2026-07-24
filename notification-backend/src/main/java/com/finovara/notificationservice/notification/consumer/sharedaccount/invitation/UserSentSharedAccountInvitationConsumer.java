package com.finovara.notificationservice.notification.consumer.sharedaccount.invitation;

import com.finovara.contracts.event.notification.sharedaccount.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserSentSharedAccountInvitationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserSentSharedAccountInvitationConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.shared-account.invitation-sent", groupId = "notification-shared-account-invitation-sent")
    public void handle(UserSentSharedAccountInvitationEvent event) {

        notificationPersistenceService.save(event.userId(), new UserSentSharedAccountInvitationDto(
                NotificationType.USER_SENT_SHARED_ACCOUNT_INVITATION,
                LocalDateTime.now(),
                event.inviteeUsername()
        ));
        log.info("Shared account invitation sent to: userId={}, inviteeUsername='{}'", event.userId(), event.inviteeUsername());
    }
}

