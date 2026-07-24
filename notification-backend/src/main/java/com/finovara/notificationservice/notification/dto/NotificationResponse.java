package com.finovara.notificationservice.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.limit.LimitExceededDto;
import com.finovara.notificationservice.notification.dto.limit.LimitWarningDto;
import com.finovara.notificationservice.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountDeletedDto;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountLeftDto;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.SharedAccountInvitationExpiredDto;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserAcceptSharedAccountInvitationDto;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserRejectSharedAccountInvitationDto;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserSentSharedAccountInvitationDto;

import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LimitWarningDto.class, name = "LIMIT_EXCEEDED_WARNING"),
        @JsonSubTypes.Type(value = LimitExceededDto.class, name = "LIMIT_EXCEEDED"),
        @JsonSubTypes.Type(value = PiggyBankWarningDto.class, name = "PIGGY_BANK_GOAL_APPROACHING"),
        @JsonSubTypes.Type(value = PiggyBankWarningDto.class, name = "PIGGY_BANK_GOAL_REACHED"),
        @JsonSubTypes.Type(value = UserAcceptSharedAccountInvitationDto.class, name = "USER_ACCEPT_SHARED_ACCOUNT_INVITATION"),
        @JsonSubTypes.Type(value = UserRejectSharedAccountInvitationDto.class, name = "USER_REJECT_SHARED_ACCOUNT_INVITATION"),
        @JsonSubTypes.Type(value = UserSentSharedAccountInvitationDto.class, name = "USER_SENT_SHARED_ACCOUNT_INVITATION"),
        @JsonSubTypes.Type(value = SharedAccountDeletedDto.class, name = "SHARED_ACCOUNT_DELETION"),
        @JsonSubTypes.Type(value = SharedAccountLeftDto.class, name = "SHARED_ACCOUNT_LEFT"),
        @JsonSubTypes.Type(value = SharedAccountInvitationExpiredDto.class, name = "SHARED_ACCOUNT_INVITATION_EXPIRED")
})
public interface NotificationResponse {
    NotificationType type();

    LocalDateTime createdAt();

    String deduplicationKey();
}
