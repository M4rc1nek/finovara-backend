package com.finovara.finovarabackend.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finovara.finovarabackend.notification.dto.limit.LimitExceededDto;
import com.finovara.finovarabackend.notification.dto.limit.LimitWarningDto;
import com.finovara.finovarabackend.notification.model.NotificationType;

import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LimitWarningDto.class, name = "LIMIT_EXCEEDED_WARNING"),
        @JsonSubTypes.Type(value = LimitExceededDto.class, name = "LIMIT_EXCEEDED")
})
public interface NotificationResponse {
    NotificationType type();

    LocalDate createdAt();

    String deduplicationKey();
}
