package com.finovara.notificationservice.notificationemail.model;

public interface EmailNotificationSettingRequest {
    Boolean enabled();
    String authorizationCode();
}