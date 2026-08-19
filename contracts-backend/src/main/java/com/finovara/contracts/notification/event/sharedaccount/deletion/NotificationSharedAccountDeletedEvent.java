package com.finovara.contracts.notification.event.sharedaccount.deletion;

public record NotificationSharedAccountDeletedEvent(
        Long accountId,
        Long recipientUserId,
        String deletedByUsername
) {
}