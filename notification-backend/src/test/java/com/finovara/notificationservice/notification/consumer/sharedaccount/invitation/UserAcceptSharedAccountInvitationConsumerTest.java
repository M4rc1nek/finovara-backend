package com.finovara.notificationservice.notification.consumer.sharedaccount.invitation;

import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.invitation.UserAcceptSharedAccountInvitationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class UserAcceptSharedAccountInvitationConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    private UserAcceptSharedAccountInvitationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserAcceptSharedAccountInvitationConsumer(notificationPersistenceService);
    }

    @Nested
    class Handle {

        @Test
        void shouldSaveNotificationWhenInvitationAccepted() {
            Long userId = 1L;
            String inviteeUsername = "john.doe";
            UserAcceptSharedAccountInvitationEvent event =
                    new UserAcceptSharedAccountInvitationEvent(userId, inviteeUsername);

            consumer.handle(event);

            ArgumentCaptor<UserAcceptSharedAccountInvitationDto> dtoCaptor =
                    ArgumentCaptor.forClass(UserAcceptSharedAccountInvitationDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());
            verifyNoMoreInteractions(notificationPersistenceService);
        }

        @Test
        void shouldSaveNotificationWithCorrectNotificationType() {
            Long userId = 1L;
            UserAcceptSharedAccountInvitationEvent event =
                    new UserAcceptSharedAccountInvitationEvent(userId, "jane.doe");

            consumer.handle(event);

            ArgumentCaptor<UserAcceptSharedAccountInvitationDto> dtoCaptor =
                    ArgumentCaptor.forClass(UserAcceptSharedAccountInvitationDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());

            assertThat(dtoCaptor.getValue().type())
                    .isEqualTo(NotificationType.USER_ACCEPT_SHARED_ACCOUNT_INVITATION);
        }

        @Test
        void shouldSaveNotificationWithInviteeUsernameFromEvent() {
            Long userId = 1L;
            String inviteeUsername = "alice.smith";
            UserAcceptSharedAccountInvitationEvent event =
                    new UserAcceptSharedAccountInvitationEvent(userId, inviteeUsername);

            consumer.handle(event);

            ArgumentCaptor<UserAcceptSharedAccountInvitationDto> dtoCaptor =
                    ArgumentCaptor.forClass(UserAcceptSharedAccountInvitationDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());

            assertThat(dtoCaptor.getValue().inviteeUsername()).isEqualTo(inviteeUsername);
        }

        @Test
        void shouldSaveNotificationWithCurrentTimestamp() {
            Long userId = 1L;
            LocalDateTime before = LocalDateTime.now();

            UserAcceptSharedAccountInvitationEvent event =
                    new UserAcceptSharedAccountInvitationEvent(userId, "bob.jones");
            consumer.handle(event);

            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<UserAcceptSharedAccountInvitationDto> dtoCaptor =
                    ArgumentCaptor.forClass(UserAcceptSharedAccountInvitationDto.class);
            verify(notificationPersistenceService).save(eq(userId), dtoCaptor.capture());

            assertThat(dtoCaptor.getValue().createdAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        void shouldSaveNotificationForCorrectUserId() {
            Long userId = 1L;
            UserAcceptSharedAccountInvitationEvent event =
                    new UserAcceptSharedAccountInvitationEvent(userId, "kate.brown");

            consumer.handle(event);

            verify(notificationPersistenceService).save(eq(userId), any());
        }
    }
}