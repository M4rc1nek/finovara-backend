package com.finovara.notificationservice.notificationemail.action.emailchange.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.util.emailsender.EmailNotifier;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.NotificationEmailSender;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyEmailChangeServiceTest {

    @Mock
    private NotificationEmailSettingsRepository repository;
    @Mock
    private NotificationEmailSender notificationEmailSender;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private EmailNotifier emailNotifier;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private NotifyEmailChangeService notifyEmailChangeService;

    private Long userId;
    private NotificationEmailSettings notificationEmailSettings;

    @BeforeEach
    void setup() {
        userId = 1L;
        notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setUserId(userId);
    }

    @Nested
    class SaveEmailNotificationTest {
        @ParameterizedTest
        @CsvSource({"true, ENABLED", "false, DISABLED"})
        void shouldSaveAndCreateActivity(boolean enabled, SettingActivityStatus expectedStatus) {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(notificationEmailSettings));

            notifyEmailChangeService.saveEmailNotification(userId, new NotificationEmailDto(enabled, "auth-code"));

            assertEquals(enabled, notificationEmailSettings.isNotifyOnEmailChange());
            verify(repository).save(notificationEmailSettings);

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(expectedStatus, eventCaptor.getValue().status());
        }
    }

    @Nested
    class GetEmailNotificationTest {
        @Test
        void shouldReturnEnabled() {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(notificationEmailSettings));
            notificationEmailSettings.setNotifyOnEmailChange(true);

            NotificationEmailDto dto = notifyEmailChangeService.getEmailNotification(userId);

            assertEquals(true, dto.enabled());
        }

        @Test
        void shouldReturnDisabled() {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(notificationEmailSettings));
            notificationEmailSettings.setNotifyOnEmailChange(false);

            NotificationEmailDto dto = notifyEmailChangeService.getEmailNotification(userId);

            assertEquals(false, dto.enabled());
        }
    }

    @Nested
    class SendEmailTest {
        @Test
        void shouldCallSenderToSendEmail() {
            UserEmailDataDto data = new UserEmailDataDto(userId, "john", "john@example.com");

            notifyEmailChangeService.sendEmail(userId, data);

            verify(notificationEmailSender).sendIfEnabled(eq(userId), eq(data), any(), any());
        }
    }
}
