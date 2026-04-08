package com.finovara.finovarabackend.notification.service;

import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.mapper.NotificationMapper;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.repository.NotificationRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {
    private final NotificationRepository notificationRepository;
    private final UserManagerService userManagerService;
    private final NotificationMapper notificationMapper;

    @Transactional
    public void saveAll(Long userId, List<NotificationDto> dtoList) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        List<Notification> entitiesToSave = dtoList.stream()
                .map(dto -> Notification.builder()
                        .type(dto.type())
                        .createdAt(LocalDateTime.now())
                        .userAssigned(user)
                        .build())
                .toList();
        notificationRepository.saveAll(entitiesToSave);
    }

    public List<NotificationDto> getAll(Long userId) {
        return notificationMapper.toDtoList(notificationRepository.getAllNotifications(userId));
    }

}
