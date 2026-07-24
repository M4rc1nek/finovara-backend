package com.finovara.contracts.event.finance.sharedaccount;

public record SharedAccountDeletedEvent(
        Long accountId,
        Long ownerId,
        Long memberId,
        Long remainingUserId,
        String ownerUsername,
        String ownerEmail,
        String memberUsername,
        String memberEmail
) {
}