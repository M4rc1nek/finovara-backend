package com.finovara.notificationservice.notification.dto.sharedaccount.invitation;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;

import java.time.LocalDateTime;

@JsonTypeName("USER_ACCEPT_INVITE")
public record UserAcceptInvitationDto(
        NotificationType type
        Long userId,
        LocalDateTime acceptedAt
) implements NotificationResponse {

    @Override
    public NotificationType type() {
        return null;
    }

    @Override
    public LocalDateTime createdAt() {
        return null;
    }

    @Override
    public String deduplicationKey() {
        return "";
    }
}
