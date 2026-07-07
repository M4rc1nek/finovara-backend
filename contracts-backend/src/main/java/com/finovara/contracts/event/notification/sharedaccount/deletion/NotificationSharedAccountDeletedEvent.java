package com.finovara.contracts.event.notification.sharedaccount.deletion;

public record NotificationSharedAccountDeletedEvent(
        Long accountId,
        Long recipientUserId,
        String deletedByUsername
) {
}