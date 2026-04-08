package com.finovara.finovarabackend.notification.dto;

import com.finovara.finovarabackend.notification.model.NotificationType;

import java.time.LocalDate;

public interface NotificationDto {
    NotificationType type();

    LocalDate createdAt();
}
