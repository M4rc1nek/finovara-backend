package com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.sharedaccount.event.notification.GoalAchievedNotificationEvent;
import com.finovara.contracts.user.authorization.dto.UserDataResponse;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalAchievedNotificationHandlerTest {

    private static final ActionEmailNotificationType TYPE =
            ActionEmailNotificationType.SHARED_ACCOUNT_PIGGY_BANK_GOAL_ACHIEVED;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long OTHER_ID = 3L;
    private static final String OWNER_EMAIL = "owner@finovara.com";
    private static final String MEMBER_EMAIL = "member@finovara.com";
    private static final String OWNER_NAME = "OwnerName";
    private static final String MEMBER_NAME = "MemberName";
    private static final String DEFAULT_RECIPIENT_NAME = "Użytkowniku";
    private static final String DEFAULT_TRIGGERED_BY_NAME = "Nieznany użytkownik";
    private static final BigDecimal CURRENT_AMOUNT = new BigDecimal("2E+3");
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("1500.00");
    private static final String FORMATTED_CURRENT_AMOUNT = "2000";
    private static final String FORMATTED_GOAL_AMOUNT = "1500.00";
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 10, 24, 14, 30);
    private static final String FORMATTED_OCCURRED_AT = "24.10.2026 14:30";

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private GoalAchievedNotificationEvent event;

    @Mock
    private UserDataResponse owner;

    @Mock
    private UserDataResponse member;

    @InjectMocks
    private GoalAchievedNotificationHandler handler;

    @Nested
    class Handle {

        @BeforeEach
        void setUp() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(event.currentAmount()).thenReturn(CURRENT_AMOUNT);
            when(event.goalAmount()).thenReturn(GOAL_AMOUNT);
            when(authBackendClient.getUserData(OWNER_ID)).thenReturn(owner);
            when(authBackendClient.getUserData(MEMBER_ID)).thenReturn(member);
        }

        @Test
        void shouldSendEmailToBothRecipientsWhenBothHaveEmailAndOwnerTriggeredGoal() {
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(owner.username()).thenReturn(Optional.of(OWNER_NAME));
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));
            when(member.username()).thenReturn(Optional.of(MEMBER_NAME));
            when(member.email()).thenReturn(Optional.of(MEMBER_EMAIL));

            handler.handle(event);

            verify(emailNotifier).send(TYPE, OWNER_EMAIL, Map.of(
                    "username", OWNER_NAME,
                    "triggeredByUsername", OWNER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
            verify(emailNotifier).send(TYPE, MEMBER_EMAIL, Map.of(
                    "username", MEMBER_NAME,
                    "triggeredByUsername", OWNER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
            verifyNoMoreInteractions(emailNotifier);
        }

        @Test
        void shouldUseMemberUsernameAsTriggeredByWhenMemberTriggeredGoal() {
            when(event.triggeredByUserId()).thenReturn(MEMBER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(owner.username()).thenReturn(Optional.of(OWNER_NAME));
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));
            when(member.username()).thenReturn(Optional.of(MEMBER_NAME));
            when(member.email()).thenReturn(Optional.of(MEMBER_EMAIL));

            handler.handle(event);

            verify(emailNotifier).send(TYPE, OWNER_EMAIL, Map.of(
                    "username", OWNER_NAME,
                    "triggeredByUsername", MEMBER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
            verify(emailNotifier).send(TYPE, MEMBER_EMAIL, Map.of(
                    "username", MEMBER_NAME,
                    "triggeredByUsername", MEMBER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
        }

        @Test
        void shouldTreatMemberAsTriggeringUserWhenTriggeredByIdMatchesNeitherOwnerNorMember() {
            when(event.triggeredByUserId()).thenReturn(OTHER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(owner.username()).thenReturn(Optional.of(OWNER_NAME));
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));
            when(member.username()).thenReturn(Optional.of(MEMBER_NAME));

            handler.handle(event);

            verify(emailNotifier).send(TYPE, OWNER_EMAIL, Map.of(
                    "username", OWNER_NAME,
                    "triggeredByUsername", MEMBER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
        }

        @Test
        void shouldUseDefaultTriggeredByUsernameWhenTriggeringUserHasNoUsername() {
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));

            handler.handle(event);

            verify(emailNotifier).send(TYPE, OWNER_EMAIL, Map.of(
                    "username", DEFAULT_RECIPIENT_NAME,
                    "triggeredByUsername", DEFAULT_TRIGGERED_BY_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
        }

        @Test
        void shouldUseDefaultRecipientUsernameWhenRecipientHasNoUsername() {
            when(event.triggeredByUserId()).thenReturn(MEMBER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));
            when(member.username()).thenReturn(Optional.of(MEMBER_NAME));

            handler.handle(event);

            verify(emailNotifier).send(TYPE, OWNER_EMAIL, Map.of(
                    "username", DEFAULT_RECIPIENT_NAME,
                    "triggeredByUsername", MEMBER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", FORMATTED_OCCURRED_AT));
        }

        @Test
        void shouldSkipOwnerWhenOwnerHasNoEmail() {
            when(event.triggeredByUserId()).thenReturn(MEMBER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(member.username()).thenReturn(Optional.of(MEMBER_NAME));
            when(member.email()).thenReturn(Optional.of(MEMBER_EMAIL));

            handler.handle(event);

            verify(emailNotifier, times(1)).send(eq(TYPE), eq(MEMBER_EMAIL), anyMap());
            verifyNoMoreInteractions(emailNotifier);
        }

        @Test
        void shouldSkipMemberWhenMemberHasNoEmail() {
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(owner.username()).thenReturn(Optional.of(OWNER_NAME));
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));

            handler.handle(event);

            verify(emailNotifier, times(1)).send(eq(TYPE), eq(OWNER_EMAIL), anyMap());
            verifyNoMoreInteractions(emailNotifier);
        }

        @Test
        void shouldNotSendAnyEmailWhenNeitherRecipientHasEmail() {
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);

            handler.handle(event);

            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldFormatOccurredAtWithLeadingZerosWhenDateIsAtMidnight() {
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(event.occurredAt()).thenReturn(LocalDateTime.of(2026, 1, 5, 0, 0));
            when(owner.username()).thenReturn(Optional.of(OWNER_NAME));
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));

            handler.handle(event);

            verify(emailNotifier).send(TYPE, OWNER_EMAIL, Map.of(
                    "username", OWNER_NAME,
                    "triggeredByUsername", OWNER_NAME,
                    "currentAmount", FORMATTED_CURRENT_AMOUNT,
                    "goalAmount", FORMATTED_GOAL_AMOUNT,
                    "occurredAt", "05.01.2026 00:00"));
        }

        @Test
        void shouldFetchUserDataExactlyOncePerUserWhenHandlingEvent() {
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);

            handler.handle(event);

            verify(authBackendClient, times(1)).getUserData(OWNER_ID);
            verify(authBackendClient, times(1)).getUserData(MEMBER_ID);
            verifyNoMoreInteractions(authBackendClient);
        }
    }

    @Nested
    class HandleFailures {

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> handler.handle(null));

            verifyNoInteractions(authBackendClient, emailNotifier);
        }

        @Test
        void shouldThrowExceptionWhenOwnerLookupFails() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUserData(OWNER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> handler.handle(event));

            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldThrowExceptionWhenMemberLookupFails() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(authBackendClient.getUserData(OWNER_ID)).thenReturn(owner);
            when(authBackendClient.getUserData(MEMBER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> handler.handle(event));

            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldThrowExceptionWhenCurrentAmountIsNullAndRecipientHasEmail() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUserData(OWNER_ID)).thenReturn(owner);
            when(authBackendClient.getUserData(MEMBER_ID)).thenReturn(member);
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));

            assertThrows(NullPointerException.class, () -> handler.handle(event));

            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldThrowExceptionWhenOccurredAtIsNullAndRecipientHasEmail() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(event.currentAmount()).thenReturn(CURRENT_AMOUNT);
            when(event.goalAmount()).thenReturn(GOAL_AMOUNT);
            when(authBackendClient.getUserData(OWNER_ID)).thenReturn(owner);
            when(authBackendClient.getUserData(MEMBER_ID)).thenReturn(member);
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));

            assertThrows(NullPointerException.class, () -> handler.handle(event));

            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldThrowExceptionWhenEmailNotifierFailsForOwner() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(event.triggeredByUserId()).thenReturn(OWNER_ID);
            when(event.currentAmount()).thenReturn(CURRENT_AMOUNT);
            when(event.goalAmount()).thenReturn(GOAL_AMOUNT);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(authBackendClient.getUserData(OWNER_ID)).thenReturn(owner);
            when(authBackendClient.getUserData(MEMBER_ID)).thenReturn(member);
            when(owner.email()).thenReturn(Optional.of(OWNER_EMAIL));
            doThrow(new IllegalStateException()).when(emailNotifier).send(eq(TYPE), eq(OWNER_EMAIL), anyMap());

            assertThrows(IllegalStateException.class, () -> handler.handle(event));

            verify(emailNotifier, never()).send(eq(TYPE), eq(MEMBER_EMAIL), anyMap());
        }
    }
}