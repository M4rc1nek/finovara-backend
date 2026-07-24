package com.finovara.notificationservice.notification.consumer.sharedaccount.deletion;

import com.finovara.contracts.event.notification.sharedaccount.deletion.NotificationSharedAccountLeftEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.sharedaccount.deletion.SharedAccountLeftDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SharedAccountLeftConsumerTest {

    private static final Long ACCOUNT_ID = 100L;
    private static final Long RECIPIENT_USER_ID = 1L;
    private static final String LEFT_USERNAME = "john";

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    private SharedAccountLeftConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SharedAccountLeftConsumer(notificationPersistenceService);
    }

    @Nested
    class WhenEventIsConsumed {

        private NotificationSharedAccountLeftEvent event;

        @BeforeEach
        void setUpEvent() {
            event = new NotificationSharedAccountLeftEvent(ACCOUNT_ID, RECIPIENT_USER_ID, LEFT_USERNAME);
        }

        @Test
        void shouldPersistNotificationWithCorrectRecipientAndPayload() {
            LocalDateTime before = LocalDateTime.now();

            consumer.handle(event);

            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<SharedAccountLeftDto> dtoCaptor = ArgumentCaptor.forClass(SharedAccountLeftDto.class);
            verify(notificationPersistenceService, times(1))
                    .save(eq(RECIPIENT_USER_ID), dtoCaptor.capture());

            SharedAccountLeftDto savedDto = dtoCaptor.getValue();
            assertEquals(NotificationType.SHARED_ACCOUNT_LEFT, savedDto.type());
            assertEquals(LEFT_USERNAME, savedDto.leftUsername());
            assertNotNull(savedDto.createdAt());
            assertFalse(savedDto.createdAt().isBefore(before));
            assertFalse(savedDto.createdAt().isAfter(after));
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void shouldPersistNotificationEvenWhenLeftUsernameIsBlank() {
            NotificationSharedAccountLeftEvent blankUsernameEvent =
                    new NotificationSharedAccountLeftEvent(ACCOUNT_ID, RECIPIENT_USER_ID, "");

            consumer.handle(blankUsernameEvent);

            ArgumentCaptor<SharedAccountLeftDto> dtoCaptor = ArgumentCaptor.forClass(SharedAccountLeftDto.class);
            verify(notificationPersistenceService, times(1))
                    .save(eq(RECIPIENT_USER_ID), dtoCaptor.capture());

            assertEquals("", dtoCaptor.getValue().leftUsername());
        }
    }
}