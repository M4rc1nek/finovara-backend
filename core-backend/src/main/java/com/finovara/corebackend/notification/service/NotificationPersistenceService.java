package com.finovara.corebackend.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.model.Notification;
import com.finovara.corebackend.notification.repository.NotificationRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.activityservice.contracts.model.SortType;
import com.finovara.corebackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
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

    @Value("${notification-persistence.page-size}")
    private int pageSize;

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
    public List<NotificationResponse> getUserNotifications(Long userId, SortType sortType) {
        userManagerService.getUserByIdOrThrow(userId);

        if ((sortType != SortType.NEWEST) && (sortType != SortType.OLDEST)) {
            throw new InvalidInputException("Unsupported sort type for notifications");

        }
        Pageable pageable = sortType.getPageable(pageSize);

        List<Notification> notifications = notificationRepository.findAllByUserAssignedId(userId, pageable);
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

