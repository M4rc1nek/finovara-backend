package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record UserRejectSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
