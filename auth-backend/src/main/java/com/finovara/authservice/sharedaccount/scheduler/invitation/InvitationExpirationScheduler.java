package com.finovara.authservice.sharedaccount.scheduler.invitation;

import com.finovara.authservice.sharedaccount.processor.invitation.InvitationExpirationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InvitationExpirationScheduler {

    private final InvitationExpirationProcessor invitationExpirationProcessor;

    @Scheduled(cron = "${scheduler.shared-account.invitation.expire-cron}")
    @SchedulerLock(name = "expireOverdueInvitations", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void expireOverdueInvitations() {
        invitationExpirationProcessor.expireOverdueInvitations();
        log.info("Invitations have expired and have been deleted");
    }
}