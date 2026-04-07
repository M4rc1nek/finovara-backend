package com.finovara.finovarabackend.notification.service;

import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.repository.NotificationRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {
    private final NotificationRepository notificationRepository;
    private final UserManagerService userManagerService;

    public void saveAll(Long userId, List<NotificationDto> dtoList) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        List<Notification> entities = dtoList.stream()
                .map(dto -> {
                    String businessKey = dto.type() + ":" + userId;

                    return Notification.builder()
                            .type(dto.type())
                            .createdAt(LocalDateTime.now())
                            .userAssigned(user)
                            .businessKey(businessKey)
                            .build();
                })
                .toList();

        for (Notification notification : entities) {
            if (!notificationRepository.existsByBusinessKey(notification.getBusinessKey())) {
                notificationRepository.save(notification);
            }
        }
    }
}
