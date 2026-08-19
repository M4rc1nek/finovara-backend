package com.finovara.contracts.notification.event.sharedaccount.invitation;

public record UserSentSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
