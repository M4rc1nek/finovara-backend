package com.finovara.notificationservice.notification.dto.sharedaccount.invitation;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;

import java.time.LocalDateTime;

@JsonTypeName("USER_REJECT_SHARED_ACCOUNT_INVITATION")
public record UserRejectSharedAccountInvitationDto(
        NotificationType type,
        LocalDateTime createdAt,
        String inviteeUsername
) implements NotificationResponse {

    @Override
    public String deduplicationKey() {
        return "%s:%s".formatted(type, inviteeUsername);
    }
}
