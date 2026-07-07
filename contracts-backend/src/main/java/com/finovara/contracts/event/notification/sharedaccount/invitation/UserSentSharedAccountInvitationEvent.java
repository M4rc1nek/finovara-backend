package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record UserSentSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
