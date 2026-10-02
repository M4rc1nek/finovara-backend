package com.finovara.contracts.sharedaccount.event.invitation;

public record UserRejectSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
