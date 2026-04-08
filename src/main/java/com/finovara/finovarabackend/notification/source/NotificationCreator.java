package com.finovara.finovarabackend.notification.source;

import com.finovara.finovarabackend.notification.dto.NotificationDto;

import java.util.List;

public interface NotificationCreator {
    List<NotificationDto> getNotifications(Long userId);
}
