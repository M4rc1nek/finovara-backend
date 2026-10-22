package com.finovara.authservice.sharedaccount.processor.invitation;

import com.finovara.authservice.sharedaccount.service.invitation.InvitationExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationExpirationProcessor {

    private final InvitationExpirationService invitationExpirationService;
    private static final int BATCH_SIZE = 500;

    public void expireOverdueInvitations() {
        int total = 0;
        int processed;
        do {
            processed = invitationExpirationService.expireBatch(LocalDateTime.now(), BATCH_SIZE);
            total += processed;
        } while (processed == BATCH_SIZE);

        if (total > 0) {
            log.info("Expired {} overdue shared account invitations", total);
        }
    }
}