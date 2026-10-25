package com.finovara.notificationservice.notificationemail.model;

public interface EmailNotificationTemplate {

    String getSubject();

    String getTemplatePath();

    EmailNotificationCategory getCategory();
}