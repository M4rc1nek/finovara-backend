package com.finovara.notificationservice.notification.service.sharedaccount.invitation;

import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserAcceptSharedAccountInvitationDto;
import com.finovara.notificationservice.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAcceptSharedAccountInvitationService {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "user.shared-account.accept-invitation", groupId = "notification-shared-account-invitation-accept")
    public void handle(UserAcceptSharedAccountInvitationEvent event) {

        notificationPersistenceService.save(event.userId(), new UserAcceptSharedAccountInvitationDto(
                NotificationType.USER_ACCEPT_SHARED_ACCOUNT_INVITATION,
                LocalDateTime.now(),
                event.inviteeUsername()
        ));
        log.info("Shared account invitation accepted: userId={}, inviteeUsername='{}'", event.userId(), event.inviteeUsername());
    }
}

