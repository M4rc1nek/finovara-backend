package com.finovara.contracts.event.user.delete.account;

public record SharedAccountDeletedEvent(
        Long accountId,
        Long ownerId,
        Long memberId,
        Long remainingUserId
) {
}
