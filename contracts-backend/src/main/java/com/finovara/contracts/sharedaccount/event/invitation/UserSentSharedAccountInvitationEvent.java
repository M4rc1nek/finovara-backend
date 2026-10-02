package com.finovara.contracts.sharedaccount.event.invitation;

public record UserSentSharedAccountInvitationEvent(
        Long userId,
        String inviteeUsername
) {
}
