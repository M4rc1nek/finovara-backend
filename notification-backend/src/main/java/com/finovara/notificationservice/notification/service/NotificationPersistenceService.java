package com.finovara.notificationservice.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.notificationservice.notification.model.Notification;
import com.finovara.notificationservice.notification.repository.NotificationRepository;
import com.finovara.contracts.model.SortType;
import org.springframework.transaction.annotation.Transactional;
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
    private final ObjectMapper objectMapper;

    @Value("${notification-persistence.page-size}")
    private int pageSize;

    @Transactional
    public void saveAll(Long userId, List<NotificationResponse> dtoList) {
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
                        .userId(userId)
                        .build())
                .toList();

        notificationRepository.saveAll(entitiesToSave);
    }

    @Transactional
    public void save(Long userId, NotificationResponse dto) {
        saveAll(userId, List.of(dto));
    }

    @Transactional
    public List<NotificationResponse> getUserNotifications(Long userId, SortType sortType) {

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

    @Transactional
    public void deleteAllNotifications(Long userId){
        notificationRepository.deleteByUserId(userId);
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

