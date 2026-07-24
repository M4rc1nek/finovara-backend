package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.event.notification.sharedaccount.invitation.SharedAccountInvitationExpiredEvent;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationExpirationService {

    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final UserManagerService userManagerService;
    private final OutboxService outboxService;

    @Transactional
    public void expireInvitation(SharedAccountInvitation invitation) {
        Long inviterUserId = invitation.getInviterUserId();
        Long inviteeUserId = invitation.getInviteeUserId();

        UserDataDto invitee = userManagerService.getUserDataWithProfileImg(inviteeUserId);

        sharedAccountInvitationRepository.delete(invitation);

        outboxService.save("User", inviterUserId.toString(), "notification.shared-account.invitation-expired",
                new SharedAccountInvitationExpiredEvent(inviterUserId, invitee.username()));

        log.info("Expired invitationId={}, inviterUserId={}, inviteeUserId={}",
                invitation.getId(), inviterUserId, inviteeUserId);
    }
}