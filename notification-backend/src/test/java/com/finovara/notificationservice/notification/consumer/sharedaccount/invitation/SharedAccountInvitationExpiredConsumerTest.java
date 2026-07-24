package com.finovara.notificationservice.notification.consumer.sharedaccount.invitation;

import com.finovara.contracts.event.notification.sharedaccount.invitation.SharedAccountInvitationExpiredEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.SharedAccountInvitationExpiredDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountInvitationExpiredConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @InjectMocks
    private SharedAccountInvitationExpiredConsumer consumer;

    private Long userId;
    private String inviteeUsername;
    private SharedAccountInvitationExpiredEvent event;

    @BeforeEach
    void setUp() {
        userId = 100L;
        inviteeUsername = "john_doe";
        event = mock(SharedAccountInvitationExpiredEvent.class);
    }

    @Nested
    class Handle {

        @Test
        void shouldSaveNotificationWhenEventIsValid() {
            when(event.userId()).thenReturn(userId);
            when(event.inviteeUsername()).thenReturn(inviteeUsername);

            consumer.handle(event);

            verify(notificationPersistenceService).save(eq(userId), any(SharedAccountInvitationExpiredDto.class));
        }

        @Test
        void shouldMapEventDataCorrectlyToNotificationDtoWhenHandlingEvent() {
            when(event.userId()).thenReturn(userId);
            when(event.inviteeUsername()).thenReturn(inviteeUsername);
            LocalDateTime beforeExecution = LocalDateTime.now();

            consumer.handle(event);

            ArgumentCaptor<SharedAccountInvitationExpiredDto> dtoCaptor =
                    ArgumentCaptor.forClass(SharedAccountInvitationExpiredDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());

            SharedAccountInvitationExpiredDto capturedDto = dtoCaptor.getValue();
            assertEquals(NotificationType.SHARED_ACCOUNT_INVITATION_EXPIRED, capturedDto.type());
            assertEquals(inviteeUsername, capturedDto.inviteeUsername());
            assertNotNull(capturedDto.createdAt());
            assertTrue(!capturedDto.createdAt().isBefore(beforeExecution));
        }

        @Test
        void shouldSaveNotificationWhenInviteeUsernameIsEmpty() {
            when(event.userId()).thenReturn(userId);
            when(event.inviteeUsername()).thenReturn("");

            consumer.handle(event);

            ArgumentCaptor<SharedAccountInvitationExpiredDto> dtoCaptor =
                    ArgumentCaptor.forClass(SharedAccountInvitationExpiredDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());
            assertEquals("", dtoCaptor.getValue().inviteeUsername());
        }

        @Test
        void shouldSaveNotificationWhenUserIdIsNull() {
            when(event.userId()).thenReturn(null);
            when(event.inviteeUsername()).thenReturn(inviteeUsername);

            consumer.handle(event);

            verify(notificationPersistenceService).save(eq(null), any(SharedAccountInvitationExpiredDto.class));
        }

        @Test
        void shouldSaveNotificationWhenInviteeUsernameIsNull() {
            when(event.userId()).thenReturn(userId);
            when(event.inviteeUsername()).thenReturn(null);

            consumer.handle(event);

            ArgumentCaptor<SharedAccountInvitationExpiredDto> dtoCaptor =
                    ArgumentCaptor.forClass(SharedAccountInvitationExpiredDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());
            assertEquals(null, dtoCaptor.getValue().inviteeUsername());
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> consumer.handle(null));
        }

        @Test
        void shouldThrowExceptionWhenPersistenceServiceThrowsException() {
            when(event.userId()).thenReturn(userId);
            when(event.inviteeUsername()).thenReturn(inviteeUsername);
            doThrow(new RuntimeException("Database error"))
                    .when(notificationPersistenceService).save(eq(userId), any(SharedAccountInvitationExpiredDto.class));

            assertThrows(RuntimeException.class, () -> consumer.handle(event));
        }
    }
}