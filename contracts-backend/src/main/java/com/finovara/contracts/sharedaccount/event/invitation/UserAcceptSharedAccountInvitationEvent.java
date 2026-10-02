package com.finovara.contracts.sharedaccount.event.invitation;

public record UserAcceptSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
