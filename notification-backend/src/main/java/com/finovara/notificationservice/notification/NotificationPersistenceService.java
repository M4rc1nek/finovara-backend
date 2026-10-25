package com.finovara.notificationservice.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.util.SortType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.notificationservice.notification.model.Notification;
import com.finovara.notificationservice.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public void save(Long userId, NotificationResponse dto) {
        saveAll(userId, List.of(dto));
    }

    public void saveAll(Long userId, List<NotificationResponse> dtoList) {
        if (dtoList.isEmpty()) {
            return;
        }

        List<String> incomingKeys = dtoList.stream()
                .map(NotificationResponse::deduplicationKey)
                .distinct()
                .toList();
        Set<String> knownKeys = new HashSet<>(notificationRepository.findExistingDeduplicationKeys(userId, incomingKeys));

        for (NotificationResponse dto : dtoList) {
            if (knownKeys.add(dto.deduplicationKey())) {
                insertIgnoringDuplicate(userId, dto);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId, SortType sortType, int page) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        List<Notification> notifications = switch (sortType) {
            case NEWEST -> notificationRepository.findNewestByUserId(userId, pageRequest);
            case OLDEST -> notificationRepository.findOldestByUserId(userId, pageRequest);
            default -> throw new InvalidInputException("Unsupported sort type for notifications");
        };

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

    public void deleteAllNotifications(Long userId) {
        List<Long> notificationIds = notificationRepository.findIdsByUserId(userId);
        notificationRepository.deleteAllByIdInBatch(notificationIds);
    }

    private void insertIgnoringDuplicate(Long userId, NotificationResponse dto) {
        try {
            notificationRepository.save(Notification.builder()
                    .type(dto.type())
                    .createdAt(dto.createdAt())
                    .deduplicationKey(dto.deduplicationKey())
                    .payload(toJson(dto))
                    .userId(userId)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            log.debug("Notification already exists for userId={}, deduplicationKey={}", userId, dto.deduplicationKey());
        }
    }

    private String toJson(NotificationResponse dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException exception) {
            log.error("Serialization failed for dto: {}", dto, exception);
            throw new IllegalStateException("Cannot serialize notification payload for type: " + dto.type(), exception);
        }
    }

    private NotificationResponse fromJson(String payload, Long notificationId) {
        try {
            return objectMapper.readValue(payload, NotificationResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize notification payload for notification id: " + notificationId, exception);
        }
    }
}