package com.finovara.finovarabackend.notification.source;

import com.finovara.finovarabackend.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationCreator {
    List<NotificationResponse> getNotifications(Long userId);
}
