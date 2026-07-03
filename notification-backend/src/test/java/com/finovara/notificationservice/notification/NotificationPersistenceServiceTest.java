package com.finovara.notificationservice.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.SortType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.notificationservice.notification.dto.limit.LimitWarningDto;
import com.finovara.notificationservice.notification.model.Notification;
import com.finovara.notificationservice.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationPersistenceService notificationPersistenceService;

    private ArgumentCaptor<List<Notification>> captor;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        notificationPersistenceService = new NotificationPersistenceService(notificationRepository, objectMapper);
        ReflectionTestUtils.setField(notificationPersistenceService, "pageSize", 5);
        captor = ArgumentCaptor.forClass(List.class);
    }

    @Nested
    class SaveAll {

        @Test
        void shouldSaveAllWhenNoDuplicates() {
            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId)).thenReturn(Set.of());

            NotificationResponse dto1 = createDto(1L);
            NotificationResponse dto2 = createDto(2L);

            notificationPersistenceService.saveAll(userId, List.of(dto1, dto2));

            verify(notificationRepository).saveAll(captor.capture());
            List<Notification> saved = captor.getValue();

            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(Notification::getDeduplicationKey)
                    .containsExactlyInAnyOrder(dto1.deduplicationKey(), dto2.deduplicationKey());
            assertThat(saved).extracting(Notification::getType)
                    .containsOnly(NotificationType.LIMIT_EXCEEDED_WARNING);
            assertThat(saved).extracting(Notification::getUserId)
                    .containsOnly(userId);
        }

        @Test
        void shouldSkipExistingKeys() {
            NotificationResponse dto1 = createDto(1L);
            NotificationResponse dto2 = createDto(2L);

            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId))
                    .thenReturn(Set.of(dto1.deduplicationKey()));

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
            assertThat(captor.getValue()).hasSize(1);
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
    class Save {

        @Test
        void shouldSaveSingleNotification() {
            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId)).thenReturn(Set.of());

            NotificationResponse dto = createDto(1L);
            notificationPersistenceService.save(userId, dto);

            verify(notificationRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().getFirst().getDeduplicationKey()).isEqualTo(dto.deduplicationKey());
        }

        @Test
        void shouldSkipIfAlreadyExists() {
            NotificationResponse dto = createDto(1L);
            when(notificationRepository.findAllDeduplicationKeysByUserAssignedId(userId))
                    .thenReturn(Set.of(dto.deduplicationKey()));

            notificationPersistenceService.save(userId, dto);

            verify(notificationRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }
    }

    @Nested
    class GetUserNotifications {

        @Test
        void shouldReturnNotificationsCorrectly() {
            Notification notification = createNotification(1L, validJson());
            Notification notification2 = createNotification(2L, validJson());

            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of(notification, notification2));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Objects::nonNull);
        }

        @Test
        void shouldSkipNullAndBlankPayloads() {
            Notification notification = createNotification(1L, validJson());
            Notification notification2 = createNotification(2L, null);
            Notification notification3 = createNotification(3L, "");

            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of(notification, notification2, notification3));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().deduplicationKey()).isNotNull();
        }

        @Test
        void shouldReturnEmptyWhenAllPayloadsInvalid() {
            Notification notification = createNotification(1L, null);
            Notification notification2 = createNotification(2L, "");

            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of(notification, notification2));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoNotifications() {
            when(notificationRepository.findAllByUserAssignedId(userId, SortType.NEWEST.getPageable(5)))
                    .thenReturn(List.of());

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.NEWEST);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldSupportOldestSortType() {
            Notification notification = createNotification(1L, validJson());

            when(notificationRepository.findAllByUserAssignedId(userId, SortType.OLDEST.getPageable(5)))
                    .thenReturn(List.of(notification));

            List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId, SortType.OLDEST);

            assertThat(result).hasSize(1);
        }

        @ParameterizedTest
        @EnumSource(value = SortType.class, names = {"AMOUNT_ASC", "AMOUNT_DESC"})
        void shouldThrowExceptionWhenSortTypeIsUnsupported(SortType sortType) {
            assertThrows(InvalidInputException.class, () -> notificationPersistenceService.getUserNotifications(userId, sortType));
        }
    }

    @Nested
    class DeleteNotifications{
        @Test
        void shouldCallRepositoryToDeleteNotifications() {
            notificationPersistenceService.deleteAllNotifications(userId);
            verify(notificationRepository, times(1)).deleteByUserId(userId);
        }

        @Test
        void shouldPropagateExceptionWhenRepositoryFails() {
            RuntimeException databaseException = new RuntimeException("Database connection timeout");
            doThrow(databaseException).when(notificationRepository).deleteByUserId(userId);

            assertThrows(RuntimeException.class, () -> {notificationPersistenceService.deleteAllNotifications(userId);});
            verify(notificationRepository, times(1)).deleteByUserId(userId);
        }
    }

    private NotificationResponse createDto(Long limitId) {
        return new LimitWarningDto(
                NotificationType.LIMIT_EXCEEDED_WARNING,
                LocalDateTime.now(),
                BigDecimal.valueOf(50),
                PeriodType.WEEKLY,
                limitId,
                BigDecimal.valueOf(75)
        );
    }

    private Notification createNotification(Long id, String payload) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setPayload(payload);
        return notification;
    }

    private String validJson() {
        return """
                {
                  "type": "LIMIT_EXCEEDED_WARNING",
                  "createdAt": "2026-01-01T00:00:00",
                  "limitPercentage": 50,
                  "period": "WEEKLY",
                  "limitId": 1,
                  "threshold": 75
                }
                """;
    }
}