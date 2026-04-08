package com.finovara.finovarabackend.notification.mapper;

import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.model.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    List<NotificationDto> toDtoList(List<Notification> notifications);
}
