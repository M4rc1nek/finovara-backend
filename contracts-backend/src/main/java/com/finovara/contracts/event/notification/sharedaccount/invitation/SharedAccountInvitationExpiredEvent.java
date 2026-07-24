package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record UserSharedAccountInvitationExpiredEvent(
        Long userId,
        String inviteeUsername
) {
}
