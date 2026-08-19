package com.finovara.contracts.finance.event.sharedaccount;

public record UsersCreatedSharedAccountEvent(
        Long inviterUserId,
        Long inviteeUserId
) {
}
