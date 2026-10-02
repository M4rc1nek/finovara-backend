package com.finovara.contracts.notification.event.sharedaccount.invitation;

public record SharedAccountInvitationExpiredEvent(
        Long userId,
        String inviteeUsername
) {
}
