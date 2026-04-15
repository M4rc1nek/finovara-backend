package com.finovara.finovarabackend.notification.service.persistence.getall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.repository.NotificationRepository;
import com.finovara.finovarabackend.notification.service.NotificationPersistenceService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceGetTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserManagerService userManagerService;

    private NotificationPersistenceService notificationPersistenceService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
        notificationPersistenceService = new NotificationPersistenceService(notificationRepository, userManagerService, objectMapper);
    }

    @Test
    void shouldGetUserNotificationCorrectly() {
        Notification notification1 = createNotification(1L, validJson());
        Notification notification2 = createNotification(2L, validJson());

        when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of(notification1, notification2));
        List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldSkipNullAndBlankPayloads() {
        Notification notification = createNotification(1L, validJson());
        Notification notification2 = createNotification(2L, null);
        Notification notification3 = createNotification(3L, "");

        when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of(notification, notification2, notification3));

        List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnEmptyWhenAllPayloadsInvalid() {
        Notification notification = createNotification(1L, null);
        Notification notification2 = createNotification(2L, "");

        when(notificationRepository.getAllNotifications(userId))
                .thenReturn(List.of(notification, notification2));

        List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoNotifications() {
        when(notificationRepository.getAllNotifications(userId)).thenReturn(List.of());

        List<NotificationResponse> result = notificationPersistenceService.getUserNotifications(userId);

        assertThat(result).isEmpty();
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
                      "createdAt": "2026-01-01",
                      "limitId": 1
                    }
                """;
    }
}