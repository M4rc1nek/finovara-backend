package com.finovara.contracts.event.notification.sharedaccount.deletion;

public record NotificationSharedAccountLeftEvent(
        Long accountId,
        Long recipientUserId,
        String leftUsername
) {
}