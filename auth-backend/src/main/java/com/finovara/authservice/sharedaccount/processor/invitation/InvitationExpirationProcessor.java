package com.finovara.authservice.sharedaccount.processor.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.service.invitation.InvitationExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationExpirationProcessor {

    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final InvitationExpirationService invitationExpirationService;

    public void expireOverdueInvitations() {
        List<SharedAccountInvitation> invitations = sharedAccountInvitationRepository.findAllExpired(LocalDateTime.now());

        invitations.forEach(invitationExpirationService::expireInvitation);

        if (!invitations.isEmpty()) {
            log.info("Expired {} overdue shared account invitations", invitations.size());
        }
    }
}