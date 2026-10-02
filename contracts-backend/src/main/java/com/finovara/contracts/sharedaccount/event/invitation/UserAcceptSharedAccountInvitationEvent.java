package com.finovara.contracts.notification.event.sharedaccount.invitation;

public record UserAcceptSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
