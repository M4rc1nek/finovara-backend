package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record SharedAccountInvitationExpiredEvent(
        Long userId,
        String inviteeUsername
) {
}
