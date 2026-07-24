package com.finovara.contracts.event.finance.sharedaccount;

public record UsersCreatedSharedAccountEvent(
        Long inviterUserId,
        Long inviteeUserId
) {
}
