package com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.service;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.event.finance.sharedaccount.LargeExpenseNotificationEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.util.emailsender.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LargeExpenseNotificationHandlerTest {

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    @InjectMocks
    private LargeExpenseNotificationHandler largeExpenseNotificationHandler;

    private Long triggeredByUserId;
    private Long ownerId;
    private Long memberId;
    private LargeExpenseNotificationEvent event;

    @BeforeEach
    void setUp() {
        triggeredByUserId = 10L;
        ownerId = 1L;
        memberId = 2L;
        event = new LargeExpenseNotificationEvent(ownerId, memberId, triggeredByUserId, 20L, new BigDecimal("150.00"), new BigDecimal("100.00"), LocalDateTime.of(2026, Month.JUNE, 5, 7, 8));
    }

    @Nested
    class Handle {

        @Test
        void shouldSendEmailsToBothRecipientsWhenEmailsArePresent() {
            when(authBackendClient.getUserEmailData(triggeredByUserId)).thenReturn(new UserDataResponse(triggeredByUserId, Optional.of("Jan"), Optional.of("jan@example.com")));
            when(authBackendClient.getUserEmailData(ownerId)).thenReturn(new UserDataResponse(ownerId, Optional.of("Owner"), Optional.of("owner@example.com")));
            when(authBackendClient.getUserEmailData(memberId)).thenReturn(new UserDataResponse(memberId, Optional.empty(), Optional.of("member@example.com")));

            largeExpenseNotificationHandler.handle(event);

            verify(authBackendClient).getUserEmailData(triggeredByUserId);
            verify(authBackendClient).getUserEmailData(ownerId);
            verify(authBackendClient).getUserEmailData(memberId);
            verify(emailNotifier).sendLargeExpenseDetected("owner@example.com", "Owner", "Jan", new BigDecimal("150.00"), new BigDecimal("100.00"), event.occurredAt());
            verify(emailNotifier).sendLargeExpenseDetected("member@example.com", "Użytkowniku", "Jan", new BigDecimal("150.00"), new BigDecimal("100.00"), event.occurredAt());
            verifyNoMoreInteractions(emailNotifier);
        }

        @Test
        void shouldSkipRecipientWithoutEmailAndUseDefaultTriggeredUsernameWhenMissing() {
            when(authBackendClient.getUserEmailData(triggeredByUserId)).thenReturn(new UserDataResponse(triggeredByUserId, Optional.empty(), Optional.of("jan@example.com")));
            when(authBackendClient.getUserEmailData(ownerId)).thenReturn(new UserDataResponse(ownerId, Optional.of("Owner"), Optional.empty()));
            when(authBackendClient.getUserEmailData(memberId)).thenReturn(new UserDataResponse(memberId, Optional.of("Member"), Optional.of("member@example.com")));

            largeExpenseNotificationHandler.handle(event);

            verify(authBackendClient).getUserEmailData(triggeredByUserId);
            verify(authBackendClient).getUserEmailData(ownerId);
            verify(authBackendClient).getUserEmailData(memberId);
            verify(emailNotifier).sendLargeExpenseDetected("member@example.com", "Member", "Nieznany użytkownik", new BigDecimal("150.00"), new BigDecimal("100.00"), event.occurredAt());
            verify(emailNotifier, never()).sendLargeExpenseDetected("owner@example.com", "Owner", "Nieznany użytkownik", new BigDecimal("150.00"), new BigDecimal("100.00"), event.occurredAt());
            verifyNoMoreInteractions(emailNotifier);
        }
    }
}


