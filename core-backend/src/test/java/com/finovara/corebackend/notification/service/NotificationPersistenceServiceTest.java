package com.finovara.corebackend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.dto.limit.LimitWarningDto;
import com.finovara.corebackend.notification.model.Notification;
import com.finovara.corebackend.notification.model.NotificationType;
import com.finovara.corebackend.notification.repository.NotificationRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.SortType;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        @BeforeEach
        void setUpPageSize() {
            ReflectionTestUtils.setField(notificationPersistenceService, "pageSize", 5);
        }

        @Test
        void shouldGetUserNotificationCorrectly() {
            Notification n1 = createNotification(1L, validJson());
            Notification n2 = createNotification(2L, validJson());

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of(n1, n2));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Objects::nonNull);
        }

        @Test
        void shouldSkipNullAndBlankPayloads() {
            Notification n1 = createNotification(1L, validJson());
            Notification n2 = createNotification(2L, null);
            Notification n3 = createNotification(3L, "");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of(n1, n2, n3));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().deduplicationKey()).isNotNull();
        }

        @Test
        void shouldReturnEmptyWhenAllPayloadsInvalid() {
            Notification n1 = createNotification(1L, null);
            Notification n2 = createNotification(2L, "");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of(n1, n2));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoNotifications() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of());

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = SortType.class, names = {"AMOUNT_ASC", "AMOUNT_DESC"})
        void shouldThrowExceptionWhenSortTypeIsUnsupported(SortType sortType) {
            assertThrows(InvalidInputException.class, () -> notificationPersistenceService.getUserNotifications(userId, sortType));
        }

        @Test
        void shouldSupportOldestSortType() {
            Notification n1 = createNotification(1L, validJson());

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
            when(notificationRepository.findAllByUserAssignedId(userId, SortType.OLDEST.getPageable(5)))
                    .thenReturn(List.of(n1));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.OLDEST);

            assertThat(result).hasSize(1);
        }
    }

    private NotificationResponse createDto(Long limitId) {
        return new LimitWarningDto(NotificationType.LIMIT_EXCEEDED_WARNING, LocalDateTime.now(),
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
                  "createdAt": "2026-01-01T00:00:00",
                  "limitId": 1
                }
                """;
    }
}