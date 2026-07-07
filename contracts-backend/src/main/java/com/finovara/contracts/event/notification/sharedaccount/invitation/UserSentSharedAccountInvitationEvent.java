package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record UserSentSharedAccountInvitation(
        Long userId,
        String inviteeUsername
) {
}
