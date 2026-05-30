package com.finovara.corebackend.notification.service;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.source.NotificationCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationCreator source1;

    @Mock
    private NotificationCreator source2;

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    private NotificationService notificationService;
    Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        List<NotificationCreator> sources = List.of(source1, source2);
        notificationService = new NotificationService(sources, notificationPersistenceService);
    }

    @Test
    void shouldCreateNotificationsCorrectly() {
        NotificationResponse notification1 = mock(NotificationResponse.class);
        NotificationResponse notification2 = mock(NotificationResponse.class);

        when(source1.getNotifications(userId)).thenReturn(List.of(notification1));
        when(source2.getNotifications(userId)).thenReturn(List.of(notification2));

        notificationService.createNotifications(userId);

        verify(notificationPersistenceService).saveAll(userId, List.of(notification1, notification2));
    }

    @Test
    void shouldHandleExceptionFromNotificationSource() {
        NotificationResponse notification = mock(NotificationResponse.class);

        when(source1.getNotifications(userId)).thenThrow(new RuntimeException());
        when(source2.getNotifications(userId)).thenReturn(List.of(notification));

        notificationService.createNotifications(userId);

        verify(notificationPersistenceService).saveAll(userId, List.of(notification));

        verify(source1).getNotifications(userId);
        verify(source2).getNotifications(userId);
    }
    @Test
    void shouldSaveEmptyListWhenAllSourcesFail() {
        when(source1.getNotifications(userId)).thenThrow(new RuntimeException());
        when(source2.getNotifications(userId)).thenThrow(new RuntimeException());

        notificationService.createNotifications(userId);

        verify(notificationPersistenceService).saveAll(userId, List.of());
    }
}