package com.finovara.contracts.sharedaccount.event.deletion;

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