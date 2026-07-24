package com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.service;

import com.finovara.contracts.auth.dto.UserDataResponse;
import com.finovara.contracts.event.finance.sharedaccount.GoalAchievedNotificationEvent;
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
class GoalAchievedNotificationHandlerTest {

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    @InjectMocks
    private GoalAchievedNotificationHandler goalAchievedNotificationHandler;

    private Long triggeredByUserId;
    private Long ownerId;
    private Long memberId;
    private GoalAchievedNotificationEvent event;

    @BeforeEach
    void setUp() {
        triggeredByUserId = 10L;
        ownerId = 1L;
        memberId = 2L;
        event = new GoalAchievedNotificationEvent(ownerId, memberId, triggeredByUserId, 30L, new BigDecimal("200.00"), new BigDecimal("300.00"), LocalDateTime.of(2026, Month.JUNE, 5, 7, 8));
    }

    @Nested
    class Handle {

        @Test
        void shouldSendEmailsToBothRecipientsWhenEmailsArePresent() {
            when(authBackendClient.getUserEmailData(triggeredByUserId)).thenReturn(new UserDataResponse(triggeredByUserId, Optional.of("Jan"), Optional.of("jan@example.com")));
            when(authBackendClient.getUserEmailData(ownerId)).thenReturn(new UserDataResponse(ownerId, Optional.of("Owner"), Optional.of("owner@example.com")));
            when(authBackendClient.getUserEmailData(memberId)).thenReturn(new UserDataResponse(memberId, Optional.empty(), Optional.of("member@example.com")));

            goalAchievedNotificationHandler.handle(event);

            verify(authBackendClient).getUserEmailData(triggeredByUserId);
            verify(authBackendClient).getUserEmailData(ownerId);
            verify(authBackendClient).getUserEmailData(memberId);
            verify(emailNotifier).sendGoalAchieved("owner@example.com", "Owner", "Jan", new BigDecimal("200.00"), new BigDecimal("300.00"), event.occurredAt());
            verify(emailNotifier).sendGoalAchieved("member@example.com", "Użytkowniku", "Jan", new BigDecimal("200.00"), new BigDecimal("300.00"), event.occurredAt());
            verifyNoMoreInteractions(emailNotifier);
        }

        @Test
        void shouldSkipRecipientWithoutEmailAndUseDefaultTriggeredUsernameWhenMissing() {
            when(authBackendClient.getUserEmailData(triggeredByUserId)).thenReturn(new UserDataResponse(triggeredByUserId, Optional.empty(), Optional.of("jan@example.com")));
            when(authBackendClient.getUserEmailData(ownerId)).thenReturn(new UserDataResponse(ownerId, Optional.of("Owner"), Optional.empty()));
            when(authBackendClient.getUserEmailData(memberId)).thenReturn(new UserDataResponse(memberId, Optional.of("Member"), Optional.of("member@example.com")));

            goalAchievedNotificationHandler.handle(event);

            verify(authBackendClient).getUserEmailData(triggeredByUserId);
            verify(authBackendClient).getUserEmailData(ownerId);
            verify(authBackendClient).getUserEmailData(memberId);
            verify(emailNotifier).sendGoalAchieved("member@example.com", "Member", "Nieznany użytkownik", new BigDecimal("200.00"), new BigDecimal("300.00"), event.occurredAt());
            verify(emailNotifier, never()).sendGoalAchieved("owner@example.com", "Owner", "Nieznany użytkownik", new BigDecimal("200.00"), new BigDecimal("300.00"), event.occurredAt());
            verifyNoMoreInteractions(emailNotifier);
        }
    }
}


