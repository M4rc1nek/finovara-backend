package com.finovara.notificationservice.notificationemail.model;

public interface EmailNotificationTemplate {

    String getSubject();

    String getTemplatePath();

    EmailNotificationCategory getCategory();

    default boolean requiresUserOptIn() {
        return getCategory() == EmailNotificationCategory.PREFERENCE_BASED;
    }
}