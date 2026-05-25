package com.finovara.corebackend.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finovara.corebackend.notification.dto.limit.LimitExceededDto;
import com.finovara.corebackend.notification.dto.limit.LimitWarningDto;
import com.finovara.corebackend.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.corebackend.notification.model.NotificationType;

import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LimitWarningDto.class, name = "LIMIT_EXCEEDED_WARNING"),
        @JsonSubTypes.Type(value = LimitExceededDto.class, name = "LIMIT_EXCEEDED"),
        @JsonSubTypes.Type(value = PiggyBankWarningDto.class, name = "PIGGY_BANK_GOAL_APPROACHING"),
        @JsonSubTypes.Type(value = PiggyBankWarningDto.class, name = "PIGGY_BANK_GOAL_REACHED")
})
public interface NotificationResponse {
    NotificationType type();

    LocalDateTime createdAt();

    String deduplicationKey();
}
