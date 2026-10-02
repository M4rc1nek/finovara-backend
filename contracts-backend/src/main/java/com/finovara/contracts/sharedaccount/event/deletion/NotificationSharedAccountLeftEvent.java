package com.finovara.contracts.notification.event.sharedaccount.deletion;

public record NotificationSharedAccountLeftEvent(
        Long accountId,
        Long recipientUserId,
        String leftUsername
) {
}