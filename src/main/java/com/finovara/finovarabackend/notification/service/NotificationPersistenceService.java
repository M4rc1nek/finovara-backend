package com.finovara.finovarabackend.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.repository.NotificationRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {
    private final NotificationRepository notificationRepository;
    private final UserManagerService userManagerService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveAll(Long userId, List<NotificationResponse> dtoList) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Set<String> existingKeys = new HashSet<>(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId));
        Set<String> batchKeys = new HashSet<>();

        List<Notification> entitiesToSave = dtoList.stream()
                .filter(dto -> !existingKeys.contains(dto.deduplicationKey()))
                .filter(dto -> batchKeys.add(dto.deduplicationKey()))
                .map(dto -> Notification.builder()
                        .type(dto.type())
                        .createdAt(dto.createdAt())
                        .deduplicationKey(dto.deduplicationKey())
                        .payload(toJson(dto))
                        .userAssigned(user)
                        .build())
                .toList();

        notificationRepository.saveAll(entitiesToSave);
    }

    @Transactional
    public List<NotificationResponse> getUserNotifications(Long userId) {
        userManagerService.getUserByIdOrThrow(userId);

        List<Notification> notifications = notificationRepository.getAllNotifications(userId);
        List<NotificationResponse> result = new ArrayList<>();

        for (Notification notification : notifications) {
            if (notification.getPayload() == null || notification.getPayload().isBlank()) {
                log.warn("Notification {} has empty payload and will be skipped", notification.getId());
                continue;
            }
            result.add(fromJson(notification.getPayload(), notification.getId()));
        }
        return result;
    }

    private String toJson(NotificationResponse dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed for dto: {}", dto, e);
            throw new IllegalStateException("Cannot serialize notification payload for type: " + dto.type(), e);
        }
    }

    private NotificationResponse fromJson(String payload, Long notificationId) {
        try {
            return objectMapper.readValue(payload, NotificationResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot deserialize notification payload for notification id: " + notificationId, e);
        }
    }
}

