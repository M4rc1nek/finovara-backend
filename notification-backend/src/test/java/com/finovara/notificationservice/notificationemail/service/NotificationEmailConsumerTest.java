package com.finovara.notificationservice.notificationemail.service;

import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.event.user.UserAccountDeletedEvent;
import com.finovara.contracts.event.user.UserCreatedEvent;
import com.finovara.notificationservice.kafka.NotificationEmailConsumer;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEmailConsumerTest {

    private static final Long USER_ID = 42L;
    private static final String USERNAME = "john";
    private static final String EMAIL = "john@example.com";
    private static final String SUBJECT = "Password changed";
    private static final String PASSWORD_CHANGED_TEMPLATE = "email/password-changed.html";
    private static final String EMAIL_CHANGED_TEMPLATE = "email/email-changed.html";
    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";
    private static final String ACCOUNT_DELETED_TEMPLATE = "email/account-deleted.html";

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Mock
    private NotificationEmailSettingsService notificationEmailSettingsService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private NotificationEmailConsumer notificationEmailConsumer;

    private NotificationEmailSettings settings;

    @BeforeEach
    void setUp() {
        settings = NotificationEmailSettings.builder()
                .userId(USER_ID)
                .notifyOnPasswordChange(false)
                .notifyOnUsernameChange(false)
                .notifyOnEmailChange(false)
                .notifyOnAccountDeleted(false)
                .build();
    }

    @Nested
    class HandleUserCreated {

        @Test
        void shouldCallCreateSettingsWithCorrectUserIdWhenUserCreated() {
            UserCreatedEvent event = new UserCreatedEvent(USER_ID, USERNAME, EMAIL, any());

            notificationEmailConsumer.handleUserCreated(event);

            verify(notificationEmailSettingsService).createSettingsIfNotExist(USER_ID);
        }
    }

    @Nested
    class SendEmail {

        @Test
        void shouldSendEmailWhenNotificationIsEnabled() {
            settings.setNotifyOnPasswordChange(true);
            SendEmailEvent event = sendEmailEvent(PASSWORD_CHANGED_TEMPLATE);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.sendEmail(event);

            verify(emailTemplateService).sendEmail(EMAIL, SUBJECT, PASSWORD_CHANGED_TEMPLATE, USERNAME, EMAIL);
        }

        @Test
        void shouldNotSendEmailWhenNotificationIsDisabled() {
            settings.setNotifyOnPasswordChange(false);
            SendEmailEvent event = sendEmailEvent(PASSWORD_CHANGED_TEMPLATE);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.sendEmail(event);

            verifyNoInteractions(emailTemplateService);
        }

        @Test
        void shouldNotSendEmailWhenUserNotFound() {
            SendEmailEvent event = sendEmailEvent(PASSWORD_CHANGED_TEMPLATE);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            notificationEmailConsumer.sendEmail(event);

            verifyNoInteractions(emailTemplateService);
        }

        @Test
        void shouldSendEmailWhenEmailChangeNotificationIsEnabled() {
            settings.setNotifyOnEmailChange(true);
            SendEmailEvent event = sendEmailEvent(EMAIL_CHANGED_TEMPLATE);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.sendEmail(event);

            verify(emailTemplateService).sendEmail(EMAIL, SUBJECT, EMAIL_CHANGED_TEMPLATE, USERNAME, EMAIL);
        }

        @Test
        void shouldSendEmailWhenUsernameChangeNotificationIsEnabled() {
            settings.setNotifyOnUsernameChange(true);
            SendEmailEvent event = sendEmailEvent(USERNAME_CHANGED_TEMPLATE);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.sendEmail(event);

            verify(emailTemplateService).sendEmail(EMAIL, SUBJECT, USERNAME_CHANGED_TEMPLATE, USERNAME, EMAIL);
        }

        @Test
        void shouldSendEmailWhenAccountDeletedNotificationIsEnabled() {
            settings.setNotifyOnAccountDeleted(true);
            SendEmailEvent event = sendEmailEvent(ACCOUNT_DELETED_TEMPLATE);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.sendEmail(event);

            verify(emailTemplateService).sendEmail(EMAIL, SUBJECT, ACCOUNT_DELETED_TEMPLATE, USERNAME, EMAIL);
        }

        @Test
        void shouldSendEmailForUnknownTemplateWhenUserExists() {
            String unknownTemplate = "email/unknown.html";
            SendEmailEvent event = sendEmailEvent(unknownTemplate);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.sendEmail(event);

            verify(emailTemplateService).sendEmail(EMAIL, SUBJECT, unknownTemplate, USERNAME, EMAIL);
        }
    }

    @Nested
    class DeleteSettings {

        @Test
        void shouldCallDeleteSettingsWhenUserAccountDeleted() {
            UserAccountDeletedEvent event = new UserAccountDeletedEvent(USER_ID);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

            notificationEmailConsumer.deleteSettings(event);

            verify(notificationEmailSettingsService).deleteSettings(settings);
        }

        @Test
        void shouldNotCallDeleteSettingsWhenUserNotFound() {
            UserAccountDeletedEvent event = new UserAccountDeletedEvent(USER_ID);
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            notificationEmailConsumer.deleteSettings(event);

            verify(notificationEmailSettingsService, never()).deleteSettings(any());
        }
    }

    private SendEmailEvent sendEmailEvent(String templateName) {
        return new SendEmailEvent(USER_ID, USERNAME, EMAIL, SUBJECT, templateName);
    }
}
