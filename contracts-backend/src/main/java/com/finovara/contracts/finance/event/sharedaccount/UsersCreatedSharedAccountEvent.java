package com.finovara.contracts.finance.event;

public record UsersCreatedSharedAccountEvent(
        Long inviterUserId,
        Long inviteeUserId
) {
}
