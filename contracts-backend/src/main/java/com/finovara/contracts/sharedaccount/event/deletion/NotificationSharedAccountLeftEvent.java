package com.finovara.contracts.sharedaccount.event.deletion;

public record NotificationSharedAccountLeftEvent(
        Long accountId,
        Long recipientUserId,
        String leftUsername
) {
}