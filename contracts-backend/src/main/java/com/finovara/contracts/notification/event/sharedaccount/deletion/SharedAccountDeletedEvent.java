package com.finovara.contracts.notification.event.sharedaccount.deletion;

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