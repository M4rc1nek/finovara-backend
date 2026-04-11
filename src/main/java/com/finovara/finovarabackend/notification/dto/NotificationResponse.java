package com.finovara.finovarabackend.notification.dto;

import com.finovara.finovarabackend.notification.dto.limit.LimitNotificationDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDate;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LimitNotificationDto.class, name = "LIMIT_EXCEEDED_WARNING")
})
public interface NotificationResponse {
    NotificationType type();

    LocalDate createdAt();
}
