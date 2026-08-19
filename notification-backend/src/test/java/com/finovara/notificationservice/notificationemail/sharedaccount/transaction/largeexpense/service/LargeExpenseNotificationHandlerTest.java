package com.finovara.notificationservice.notificationemail.sharedaccount.transaction.largeexpense.service;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.finance.event.sharedaccount.LargeExpenseNotificationEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LargeExpenseNotificationHandlerTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long TRIGGERED_BY_USER_ID = 3L;
    private static final Long EXPENSE_ID = 200L;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private LargeExpenseNotificationEvent event;

    @Mock
    private UserDataResponse triggeredByUser;

    @Mock
    private UserDataResponse ownerUser;

    @Mock
    private UserDataResponse memberUser;

    private LargeExpenseNotificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LargeExpenseNotificationHandler(authBackendClient, emailNotifier);
        when(event.triggeredByUserId()).thenReturn(TRIGGERED_BY_USER_ID);
        when(event.ownerId()).thenReturn(OWNER_ID);
        when(event.memberId()).thenReturn(MEMBER_ID);
        when(event.expenseId()).thenReturn(EXPENSE_ID);
        when(event.amount()).thenReturn(new BigDecimal("2500.50"));
        when(event.threshold()).thenReturn(new BigDecimal("2000.00"));
        when(event.occurredAt()).thenReturn(LocalDateTime.of(2024, 6, 15, 9, 0));
        when(authBackendClient.getUserEmailData(TRIGGERED_BY_USER_ID)).thenReturn(triggeredByUser);
    }

    @Nested
    class Handle {

        @Test
        void shouldSendEmailsToBothRecipientsWhenBothHaveEmail() {
            when(triggeredByUser.username()).thenReturn(Optional.of("trigger-user"));
            when(authBackendClient.getUserEmailData(OWNER_ID)).thenReturn(ownerUser);
            when(authBackendClient.getUserEmailData(MEMBER_ID)).thenReturn(memberUser);
            when(ownerUser.email()).thenReturn(Optional.of("owner@example.com"));
            when(ownerUser.username()).thenReturn(Optional.of("owner"));
            when(memberUser.email()).thenReturn(Optional.of("member@example.com"));
            when(memberUser.username()).thenReturn(Optional.of("member"));

            handler.handle(event);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.LARGE_EXPENSE_DETECTED), eq("owner@example.com"), any());
            verify(emailNotifier).send(eq(ActionEmailNotificationType.LARGE_EXPENSE_DETECTED), eq("member@example.com"), any());
        }

        @Test
        void shouldSkipOwnerEmailWhenOwnerHasNoEmail() {
            when(triggeredByUser.username()).thenReturn(Optional.of("trigger-user"));
            when(authBackendClient.getUserEmailData(OWNER_ID)).thenReturn(ownerUser);
            when(authBackendClient.getUserEmailData(MEMBER_ID)).thenReturn(memberUser);
            when(ownerUser.email()).thenReturn(Optional.empty());
            when(memberUser.email()).thenReturn(Optional.of("member@example.com"));
            when(memberUser.username()).thenReturn(Optional.of("member"));

            handler.handle(event);

            verify(emailNotifier, never()).send(any(), eq("owner@example.com"), any());
            verify(emailNotifier, times(1)).send(any(), eq("member@example.com"), any());
        }

        @Test
        void shouldSkipMemberEmailWhenMemberHasNoEmail() {
            when(triggeredByUser.username()).thenReturn(Optional.of("trigger-user"));
            when(authBackendClient.getUserEmailData(OWNER_ID)).thenReturn(ownerUser);
            when(authBackendClient.getUserEmailData(MEMBER_ID)).thenReturn(memberUser);
            when(ownerUser.email()).thenReturn(Optional.of("owner@example.com"));
            when(ownerUser.username()).thenReturn(Optional.of("owner"));
            when(memberUser.email()).thenReturn(Optional.empty());

            handler.handle(event);

            verify(emailNotifier, times(1)).send(any(), eq("owner@example.com"), any());
            verify(emailNotifier, never()).send(any(), eq("member@example.com"), any());
        }

        @Test
        void shouldUseDefaultTriggeredByUsernameWhenTriggeredByUsernameMissing() {
            when(triggeredByUser.username()).thenReturn(Optional.empty());
            when(authBackendClient.getUserEmailData(OWNER_ID)).thenReturn(ownerUser);
            when(authBackendClient.getUserEmailData(MEMBER_ID)).thenReturn(memberUser);
            when(ownerUser.email()).thenReturn(Optional.of("owner@example.com"));
            when(ownerUser.username()).thenReturn(Optional.of("owner"));
            when(memberUser.email()).thenReturn(Optional.empty());

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            handler.handle(event);

            verify(emailNotifier).send(any(), eq("owner@example.com"), captor.capture());
            assertEquals("Nieznany użytkownik", captor.getValue().get("triggeredByUsername"));
        }

        @Test
        void shouldUseDefaultRecipientUsernameWhenRecipientUsernameMissing() {
            when(triggeredByUser.username()).thenReturn(Optional.of("trigger-user"));
            when(authBackendClient.getUserEmailData(OWNER_ID)).thenReturn(ownerUser);
            when(authBackendClient.getUserEmailData(MEMBER_ID)).thenReturn(memberUser);
            when(ownerUser.email()).thenReturn(Optional.of("owner@example.com"));
            when(ownerUser.username()).thenReturn(Optional.empty());
            when(memberUser.email()).thenReturn(Optional.empty());

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            handler.handle(event);

            verify(emailNotifier).send(any(), eq("owner@example.com"), captor.capture());
            assertEquals("Użytkowniku", captor.getValue().get("username"));
        }

        @Test
        void shouldIncludeCorrectPlaceholdersInEmail() {
            when(triggeredByUser.username()).thenReturn(Optional.of("trigger-user"));
            when(authBackendClient.getUserEmailData(OWNER_ID)).thenReturn(ownerUser);
            when(authBackendClient.getUserEmailData(MEMBER_ID)).thenReturn(memberUser);
            when(ownerUser.email()).thenReturn(Optional.of("owner@example.com"));
            when(ownerUser.username()).thenReturn(Optional.of("owner"));
            when(memberUser.email()).thenReturn(Optional.empty());

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            handler.handle(event);

            verify(emailNotifier).send(any(), eq("owner@example.com"), captor.capture());
            Map<String, String> placeholders = captor.getValue();
            assertEquals("owner", placeholders.get("username"));
            assertEquals("trigger-user", placeholders.get("triggeredByUsername"));
            assertEquals("2500.50", placeholders.get("amount"));
            assertEquals("2000.00", placeholders.get("threshold"));
            assertEquals("15.06.2024 09:00", placeholders.get("occurredAt"));
        }
    }
}