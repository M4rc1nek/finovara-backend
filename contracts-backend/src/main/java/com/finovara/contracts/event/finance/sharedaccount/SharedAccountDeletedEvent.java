package com.finovara.contracts.event.finance.sharedaccount;

public record SharedAccountDeletedEvent(
        Long accountId,
        Long ownerId,
        Long memberId,
        Long remainingUserId
) {
}
