package com.finovara.contracts.sharedaccount.event.invitation;

public record SharedAccountInvitationExpiredEvent(
        Long userId,
        String inviteeUsername
) {
}
