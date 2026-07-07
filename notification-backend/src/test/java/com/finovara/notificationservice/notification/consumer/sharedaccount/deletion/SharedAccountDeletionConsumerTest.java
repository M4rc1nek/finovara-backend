package com.finovara.notificationservice.notification.consumer.sharedaccount.deletion;

import com.finovara.contracts.event.notification.sharedaccount.deletion.NotificationSharedAccountDeletedEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountDeletedDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @Mock
    private NotificationSharedAccountDeletedEvent event;

    private SharedAccountDeletionConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SharedAccountDeletionConsumer(notificationPersistenceService);
    }

    @Test
    void shouldSaveNotificationWhenEventIsReceived() {
        Long recipientUserId = 101L;
        String deletedByUsername = "test_user";

        when(event.recipientUserId()).thenReturn(recipientUserId);
        when(event.deletedByUsername()).thenReturn(deletedByUsername);

        consumer.handle(event);

        ArgumentCaptor<SharedAccountDeletedDto> dtoCaptor = ArgumentCaptor.forClass(SharedAccountDeletedDto.class);

        verify(notificationPersistenceService, times(1)).save(eq(recipientUserId), dtoCaptor.capture());

        SharedAccountDeletedDto capturedDto = dtoCaptor.getValue();
        assertThat(capturedDto.type()).isEqualTo(NotificationType.SHARED_ACCOUNT_DELETED);
        assertThat(capturedDto.deletedByUsername()).isEqualTo(deletedByUsername);
        assertThat(capturedDto.createdAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldHandleNullValuesInEvent() {
        when(event.recipientUserId()).thenReturn(null);
        when(event.deletedByUsername()).thenReturn(null);

        consumer.handle(event);

        ArgumentCaptor<SharedAccountDeletedDto> dtoCaptor = ArgumentCaptor.forClass(SharedAccountDeletedDto.class);

        verify(notificationPersistenceService, times(1)).save(eq(null), dtoCaptor.capture());

        SharedAccountDeletedDto capturedDto = dtoCaptor.getValue();
        assertThat(capturedDto.type()).isEqualTo(NotificationType.SHARED_ACCOUNT_DELETED);
        assertThat(capturedDto.deletedByUsername()).isNull();
    }

    @Test
    void shouldProcessMultipleEventsIndependently() {
        Long userId1 = 1L;
        String user1 = "user_one";
        Long userId2 = 2L;
        String user2 = "user_two";

        when(event.recipientUserId()).thenReturn(userId1).thenReturn(userId2);
        when(event.deletedByUsername()).thenReturn(user1).thenReturn(user2);

        consumer.handle(event);
        consumer.handle(event);

        verify(notificationPersistenceService, times(2)).save(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(SharedAccountDeletedDto.class)
        );
    }
}