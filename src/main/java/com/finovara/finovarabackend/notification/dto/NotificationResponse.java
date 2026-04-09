package com.finovara.finovarabackend.notification.dto;

import com.finovara.finovarabackend.notification.model.NotificationType;

import java.time.LocalDate;

public interface NotificationResponse {
    NotificationType type();

    LocalDate createdAt();
}
