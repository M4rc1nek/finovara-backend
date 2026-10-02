package com.finovara.contracts.sharedaccount.event.deletion;

public record NotificationSharedAccountDeletedEvent(
        Long accountId,
        Long recipientUserId,
        String deletedByUsername
) {
}