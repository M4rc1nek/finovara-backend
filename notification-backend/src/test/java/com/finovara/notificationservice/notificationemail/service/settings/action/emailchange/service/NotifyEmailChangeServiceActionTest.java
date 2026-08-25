package com.finovara.notificationservice.notificationemail.service.settings.action.emailchange.service;

import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyEmailChangeServiceActionTest {

    private static final Long USER_ID = 42L;
    private static final String TOPIC = "activity.settings";

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private NotificationEmailSettings notificationEmailSettings;

    private NotifyEmailChangeServiceAction service;

    @BeforeEach
    void setUp() {
        service = new NotifyEmailChangeServiceAction(notificationEmailSettingsRepository, authBackendClient, kafkaTemplate, additionalAuthorizationCodeResolver);
    }

    @Nested
    class ApplySetting {

        @Test
        void shouldSetNotifyOnEmailChangeToTrue() {
            NotificationEmailDto dto = new NotificationEmailDto(true, null);

            service.applySetting(notificationEmailSettings, dto);

            verify(notificationEmailSettings).setNotifyOnEmailChange(true);
        }

        @Test
        void shouldSetNotifyOnEmailChangeToFalse() {
            NotificationEmailDto dto = new NotificationEmailDto(false, null);

            service.applySetting(notificationEmailSettings, dto);

            verify(notificationEmailSettings).setNotifyOnEmailChange(false);
        }
    }

    @Nested
    class MapToDto {

        @Test
        void shouldMapEnabledTrueAndNullAuthorizationCode() {
            when(notificationEmailSettings.isNotifyOnEmailChange()).thenReturn(true);

            NotificationEmailDto result = service.mapToDto(notificationEmailSettings);

            assertTrue(result.enabled());
            assertNull(result.authorizationCode());
        }

        @Test
        void shouldMapEnabledFalse() {
            when(notificationEmailSettings.isNotifyOnEmailChange()).thenReturn(false);

            NotificationEmailDto result = service.mapToDto(notificationEmailSettings);

            assertFalse(result.enabled());
        }
    }

    @Nested
    class HandleActivity {

        @Test
        void shouldSendEnabledActivityEventToKafka() {
            service.handleActivity(USER_ID, true);

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
            SettingsActivityEvent event = eventCaptor.getValue();
            assertEquals(USER_ID, event.userId());
            assertEquals(SettingType.NOTIFICATION_EMAIL_CHANGED, event.settingType());
            assertEquals(SettingActivityStatus.ENABLED, event.status());
        }

        @Test
        void shouldSendDisabledActivityEventToKafka() {
            service.handleActivity(USER_ID, false);

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
            SettingsActivityEvent event = eventCaptor.getValue();
            assertEquals(SettingActivityStatus.DISABLED, event.status());
        }
    }

    @Nested
    class SaveEmailNotificationIntegration {

        @Test
        void shouldPersistSettingsAndPublishActivityEventWhenSettingsExist() {
            NotificationEmailDto dto = new NotificationEmailDto(true, "code");
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            service.saveEmailNotification(USER_ID, dto);

            verify(notificationEmailSettings).setNotifyOnEmailChange(true);
            verify(notificationEmailSettingsRepository).save(notificationEmailSettings);
            verify(kafkaTemplate).send(eq(TOPIC), any(SettingsActivityEvent.class));
        }
    }
}