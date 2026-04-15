package com.finovara.finovarabackend.notification.service.persistence.saveall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.dto.limit.LimitWarningDto;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.repository.NotificationRepository;
import com.finovara.finovarabackend.notification.service.NotificationPersistenceService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
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
class NotificationPersistenceSaveAllTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserManagerService userManagerService;

    private NotificationPersistenceService notificationPersistenceService;

    private ArgumentCaptor<List<Notification>> captor;
    private Long userId;

    @BeforeEach
    void SetUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        userId = 1L;
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());

        notificationPersistenceService = new NotificationPersistenceService(notificationRepository, userManagerService, objectMapper);
        captor = ArgumentCaptor.forClass(List.class);
    }

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

        List<Notification> saved = captor.getValue();

        assertThat(saved).isEmpty();
    }

    private NotificationResponse createDto(Long limitId) {
        return new LimitWarningDto(NotificationType.LIMIT_EXCEEDED_WARNING,
                LocalDate.now(),
                BigDecimal.valueOf(50),
                PeriodType.WEEKLY,
                limitId,
                BigDecimal.valueOf(100));
    }
}