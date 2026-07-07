package com.finovara.contracts.event.notification.sharedaccount.invitation;

public record NotificationSharedAccountDeletedEvent(
        Long id,
        Long ownerId,
        Long memberId
) {
}
