package com.finovara.notificationservice.notificationemail.service.settings;

import com.finovara.contracts.notification.email.ActionEmailEventType;
import com.finovara.contracts.notification.event.SendEmailEvent;
import com.finovara.contracts.user.event.account.delete.UserAccountDeletedEvent;
import com.finovara.contracts.user.event.UserCreatedEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingEmailConsumerTest {

    private static final Long USER_ID = 10L;
    private static final String RECIPIENT_EMAIL = "user@example.com";
    private static final String USERNAME = "john";

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Mock
    private NotificationEmailSettingsService notificationEmailSettingsService;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private NotificationEmailSettings settings;

    @Mock
    private SendEmailEvent sendEmailEvent;

    @Mock
    private UserCreatedEvent userCreatedEvent;

    @Mock
    private UserAccountDeletedEvent userAccountDeletedEvent;

    private NotificationSettingEmailConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationSettingEmailConsumer(notificationEmailSettingsRepository, notificationEmailSettingsService, emailNotifier, authBackendClient);
    }

    @Nested
    class HandleUserCreated {

        @Test
        void shouldCreateSettingsWhenUserCreatedEventReceived() {
            when(userCreatedEvent.userId()).thenReturn(USER_ID);

            consumer.handleUserCreated(userCreatedEvent);

            verify(notificationEmailSettingsService).createSettingsIfNotExist(USER_ID);
        }
    }

    @Nested
    class SendEmail {

        @BeforeEach
        void setUp() {
            when(sendEmailEvent.userId()).thenReturn(USER_ID);
            when(sendEmailEvent.email()).thenReturn(RECIPIENT_EMAIL);
            when(sendEmailEvent.username()).thenReturn(USERNAME);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        }

        @Test
        void shouldSendEmailWhenEmailChangedNotificationEnabled() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.EMAIL_CHANGED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());
            when(settings.isNotifyOnEmailChange()).thenReturn(true);

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.EMAIL_CHANGED), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldSendEmailWhenPasswordChangedNotificationEnabled() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.PASSWORD_CHANGED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());
            when(settings.isNotifyOnPasswordChange()).thenReturn(true);

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.PASSWORD_CHANGED), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldSendEmailWhenUsernameChangedNotificationEnabled() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.USERNAME_CHANGED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());
            when(settings.isNotifyOnUsernameChange()).thenReturn(true);

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.USERNAME_CHANGED), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldSendEmailWhenAccountDeletedNotificationEnabled() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.ACCOUNT_DELETED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());
            when(settings.isNotifyOnAccountDeleted()).thenReturn(true);

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.ACCOUNT_DELETED), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldAlwaysSendEmailWhenLargeExpenseDetectedRegardlessOfSettings() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.SHARED_ACCOUNT_LARGE_EXPENSE_DETECTED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.SHARED_ACCOUNT_LARGE_EXPENSE_DETECTED), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldAlwaysSendEmailWhenPiggyBankGoalAchievedRegardlessOfSettings() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.SHARED_ACCOUNT_PIGGY_BANK_GOAL_ACHIEVED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.SHARED_ACCOUNT_PIGGY_BANK_GOAL_ACHIEVED), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldMergeUsernameAndEmailIntoPlaceholdersWhenAbsent() {
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.EMAIL_CHANGED);
            when(sendEmailEvent.placeholders()).thenReturn(new HashMap<>());
            when(settings.isNotifyOnEmailChange()).thenReturn(true);

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(any(), eq(RECIPIENT_EMAIL), captor.capture());
            assertEquals(USERNAME, captor.getValue().get("username"));
            assertEquals(RECIPIENT_EMAIL, captor.getValue().get("email"));
        }

        @Test
        void shouldNotOverrideExistingPlaceholdersWithDefaults() {
            Map<String, String> existingPlaceholders = new HashMap<>();
            existingPlaceholders.put("username", "custom-username");
            when(sendEmailEvent.eventType()).thenReturn(ActionEmailEventType.EMAIL_CHANGED);
            when(sendEmailEvent.placeholders()).thenReturn(existingPlaceholders);
            when(settings.isNotifyOnEmailChange()).thenReturn(true);

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            consumer.sendEmail(sendEmailEvent);

            verify(emailNotifier).send(any(), eq(RECIPIENT_EMAIL), captor.capture());
            assertEquals("custom-username", captor.getValue().get("username"));
        }
    }

    @Nested
    class DeleteSettings {

        @Test
        void shouldDeleteSettingsWhenSettingsExistForUser() {
            when(userAccountDeletedEvent.userId()).thenReturn(USER_ID);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            consumer.deleteSettings(userAccountDeletedEvent);

            verify(notificationEmailSettingsService).deleteSettings(settings);
        }

        @Test
        void shouldNotDeleteSettingsWhenSettingsNotFoundForUser() {
            when(userAccountDeletedEvent.userId()).thenReturn(USER_ID);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            consumer.deleteSettings(userAccountDeletedEvent);

            verify(notificationEmailSettingsService, never()).deleteSettings(any());
        }
    }
}