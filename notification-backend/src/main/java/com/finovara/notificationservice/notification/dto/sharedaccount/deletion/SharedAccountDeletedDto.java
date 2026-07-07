package com.finovara.notificationservice.notification.dto.sharedaccount.deletion;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;

import java.time.LocalDateTime;

@JsonTypeName("SHARED_ACCOUNT_DELETION")
public record SharedAccountDeletedDto(
        NotificationType type,
        LocalDateTime createdAt,
        String deletedByUsername
) implements NotificationResponse {

    @Override
    public String deduplicationKey() {
        return "%s:%s".formatted(type, deletedByUsername);
    }
}