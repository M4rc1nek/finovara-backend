package com.finovara.notificationservice.notificationemail.dto;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationSettingRequest;

public record NotificationEmailDto(
        Boolean enabled,
        String authorizationCode
) implements EmailNotificationSettingRequest {
}