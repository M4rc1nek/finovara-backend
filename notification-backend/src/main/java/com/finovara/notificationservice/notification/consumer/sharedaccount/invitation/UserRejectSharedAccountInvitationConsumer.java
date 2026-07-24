package com.finovara.notificationservice.notification.consumer.sharedaccount.invitation;

import com.finovara.contracts.event.notification.sharedaccount.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserRejectSharedAccountInvitationDto;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserRejectSharedAccountInvitationConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "user.shared-account.reject-invitation", groupId = "notification-shared-account-invitation-reject")
    public void handle(UserRejectSharedAccountInvitationEvent event) {

        notificationPersistenceService.save(event.userId(), new UserRejectSharedAccountInvitationDto(
                NotificationType.USER_REJECT_SHARED_ACCOUNT_INVITATION,
                LocalDateTime.now(),
                event.inviteeUsername()
        ));
        log.info("Shared account invitation rejected: userId={}, inviteeUsername='{}'", event.userId(), event.inviteeUsername());
    }
}

