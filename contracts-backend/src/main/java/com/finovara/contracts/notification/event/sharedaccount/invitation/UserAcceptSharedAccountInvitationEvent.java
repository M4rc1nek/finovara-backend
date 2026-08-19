package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record UserAcceptSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
