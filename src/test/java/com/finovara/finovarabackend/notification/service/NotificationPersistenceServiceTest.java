package com.finovara.finovarabackend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.dto.limit.LimitWarningDto;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.repository.NotificationRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserManagerService userManagerService;

    private NotificationPersistenceService notificationPersistenceService;

    private ArgumentCaptor<List<Notification>> captor;

    private Long userId;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        userId = 1L;

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());

        notificationPersistenceService = new NotificationPersistenceService(notificationRepository, userManagerService, objectMapper);

        captor = ArgumentCaptor.forClass(List.class);
    }

    @Nested
    class SaveAllNotification {
        @Test
        void shouldSaveAllWhenNoDuplicates() {
            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId)).thenReturn(Set.of());

            NotificationResponse dto1 = createDto(1L);
            NotificationResponse dto2 = createDto(2L);

            notificationPersistenceService.saveAll(userId, List.of(dto1, dto2));

            verify(notificationRepository).saveAll(captor.capture());

            List<Notification> saved = captor.getValue();

            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(Notification::getDeduplicationKey).containsExactlyInAnyOrder(dto1.deduplicationKey(), dto2.deduplicationKey());

            assertThat(saved).extracting(Notification::getType).containsOnly(NotificationType.LIMIT_EXCEEDED_WARNING);
        }

        @Test
        void shouldSkipExistingKeys() {
            NotificationResponse dto1 = createDto(1L);
            NotificationResponse dto2 = createDto(2L);

            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId)).thenReturn(Set.of(dto1.deduplicationKey()));

            notificationPersistenceService.saveAll(userId, List.of(dto1, dto2));

            verify(notificationRepository).saveAll(captor.capture());

            List<Notification> saved = captor.getValue();

            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().getDeduplicationKey()).isEqualTo(dto2.deduplicationKey());
        }

        @Test
        void shouldRemoveDuplicatesInBatch() {
            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId)).thenReturn(Set.of());

            NotificationResponse dto1 = createDto(1L);
            NotificationResponse dto2 = createDto(1L);

            notificationPersistenceService.saveAll(userId, List.of(dto1, dto2));

            verify(notificationRepository).saveAll(captor.capture());

            List<Notification> saved = captor.getValue();

            assertThat(saved).hasSize(1);
        }

        @Test
        void shouldHandleEmptyInput() {
            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId)).thenReturn(Set.of());

            notificationPersistenceService.saveAll(userId, List.of());

            verify(notificationRepository).saveAll(captor.capture());

            assertThat(captor.getValue()).isEmpty();
        }
    }

    @Nested
    class GetUserNotifications {
        @Test
        void shouldGetUserNotificationCorrectly() {
            Notification n1 = createNotification(1L, validJson());
            Notification n2 = createNotification(2L, validJson());

            when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of(n1, n2));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

            assertThat(result).hasSize(2);
        }

        @Test
        void shouldSkipNullAndBlankPayloads() {
            Notification n1 = createNotification(1L, validJson());
            Notification n2 = createNotification(2L, null);
            Notification n3 = createNotification(3L, "");

            when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of(n1, n2, n3));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

            assertThat(result).hasSize(1);
        }

        @Test
        void shouldReturnEmptyWhenAllPayloadsInvalid() {
            Notification n1 = createNotification(1L, null);
            Notification n2 = createNotification(2L, "");

            when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of(n1, n2));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoNotifications() {
            when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of());

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

            assertThat(result).isEmpty();
        }
    }

    private NotificationResponse createDto(Long limitId) {
        return new LimitWarningDto(NotificationType.LIMIT_EXCEEDED_WARNING, LocalDate.now(),
                BigDecimal.valueOf(50), PeriodType.WEEKLY, limitId, BigDecimal.valueOf(100));
    }

    private Notification createNotification(Long id, String payload) {
        Notification n = new Notification();
        n.setId(id);
        n.setPayload(payload);
        return n;
    }

    private String validJson() {
        return """
                {
                  "type": "LIMIT_EXCEEDED_WARNING",
                  "createdAt": "2026-01-01",
                  "limitId": 1
                }
                """;
    }
}