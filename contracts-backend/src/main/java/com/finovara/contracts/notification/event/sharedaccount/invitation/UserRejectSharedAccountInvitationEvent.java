package com.finovara.contracts.notification.event.sharedaccount.invitation;

public record UserRejectSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
