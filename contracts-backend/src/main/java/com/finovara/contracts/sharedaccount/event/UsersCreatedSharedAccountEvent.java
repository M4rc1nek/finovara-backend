package com.finovara.contracts.sharedaccount.event;

public record UsersCreatedSharedAccountEvent(
        Long inviterUserId,
        Long inviteeUserId
) {
}
