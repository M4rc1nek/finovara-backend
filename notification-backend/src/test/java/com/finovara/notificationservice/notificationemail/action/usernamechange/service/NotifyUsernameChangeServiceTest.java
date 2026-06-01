package com.finovara.notificationservice.notificationemail.action.usernamechange.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.NotificationEmailSender;
import com.finovara.notificationservice.notificationemail.util.emailsender.UsernameChangeNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class NotifyUsernameChangeServiceTest {

    @Mock
    private NotificationEmailSettingsRepository repository;
    @Mock
    private NotificationEmailSender notificationEmailSender;
    @Mock
    private UsernameChangeNotifier usernameChangeNotifier;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NotifyUsernameChangeService notifyUsernameChangeService;

    private Long userId;
    private NotificationEmailSettings notificationEmailSettings;

    @BeforeEach
    void setup() {
        userId = 1L;
        notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setUserId(userId);
    }

    @Nested
    class SaveUsernameChangeTest {

        @ParameterizedTest
        @CsvSource({"true, ENABLED", "false, DISABLED"})
        void shouldSaveAndCreateActivity(boolean enabled, SettingActivityStatus expectedStatus) {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(notificationEmailSettings));

            notifyUsernameChangeService.saveEmailNotification(userId, new NotificationEmailDto(enabled));

            assertEquals(enabled, notificationEmailSettings.isNotifyOnUsernameChange());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(expectedStatus, eventCaptor.getValue().status());
        }
    }

    @Nested
    class GetUsernameChangeTest {

        @Test
        void shouldReturnEnabled() {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(notificationEmailSettings));
            notificationEmailSettings.setNotifyOnUsernameChange(true);

            NotificationEmailDto dto = notifyUsernameChangeService.getEmailNotification(userId);

            assertEquals(true, dto.enabled());
        }

        @Test
        void shouldReturnDisabled() {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(notificationEmailSettings));
            notificationEmailSettings.setNotifyOnUsernameChange(false);

            NotificationEmailDto dto = notifyUsernameChangeService.getEmailNotification(userId);

            assertEquals(false, dto.enabled());
        }
    }

    @Nested
    class SendEmailTest {

        @Test
        void shouldCallSenderToSendEmail() {
            UserEmailDataDto data = new UserEmailDataDto(userId, "john", "john@example.com");

            notifyUsernameChangeService.sendEmail(userId, data);

            verify(notificationEmailSender).sendIfEnabled(eq(userId), eq(data), any(), any());
        }
    }
}
