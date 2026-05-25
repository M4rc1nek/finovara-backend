package com.finovara.corebackend.notification.source;

import com.finovara.corebackend.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationCreator {
    List<NotificationResponse> getNotifications(Long userId);
}
