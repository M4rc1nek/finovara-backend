package com.finovara.notificationservice.notificationemail.service.settings.action.accountdeleted.service;

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
class NotifyOnAccountDeletedServiceActionTest {

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

    private NotifyOnAccountDeletedServiceAction service;

    @BeforeEach
    void setUp() {
        service = new NotifyOnAccountDeletedServiceAction(notificationEmailSettingsRepository, authBackendClient, kafkaTemplate, additionalAuthorizationCodeResolver);
    }

    @Nested
    class IsEnabled {

        @Test
        void shouldReturnTrueWhenDtoEnabledIsTrue() {
            NotificationEmailDto dto = new NotificationEmailDto(true, null);

            boolean result = service.isEnabled(dto);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenDtoEnabledIsFalse() {
            NotificationEmailDto dto = new NotificationEmailDto(false, null);

            boolean result = service.isEnabled(dto);

            assertFalse(result);
        }
    }

    @Nested
    class ApplySetting {

        @Test
        void shouldSetNotifyOnAccountDeletedToTrue() {
            service.applySetting(notificationEmailSettings, true);

            verify(notificationEmailSettings).setNotifyOnAccountDeleted(true);
        }

        @Test
        void shouldSetNotifyOnAccountDeletedToFalse() {
            service.applySetting(notificationEmailSettings, false);

            verify(notificationEmailSettings).setNotifyOnAccountDeleted(false);
        }
    }

    @Nested
    class MapToDto {

        @Test
        void shouldMapEnabledTrueAndNullAuthorizationCode() {
            when(notificationEmailSettings.isNotifyOnAccountDeleted()).thenReturn(true);

            NotificationEmailDto result = service.mapToDto(notificationEmailSettings);

            assertTrue(result.enabled());
            assertNull(result.authorizationCode());
        }

        @Test
        void shouldMapEnabledFalse() {
            when(notificationEmailSettings.isNotifyOnAccountDeleted()).thenReturn(false);

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
            assertEquals(SettingType.NOTIFICATION_ACCOUNT_DELETED, event.settingType());
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

        @Test
        void shouldPublishEventForCorrectUserId() {
            service.handleActivity(USER_ID, true);

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
            assertEquals(USER_ID, eventCaptor.getValue().userId());
        }
    }

    @Nested
    class SaveEmailNotificationIntegration {

        @Test
        void shouldPersistSettingsAndPublishActivityEventWhenSettingsExist() {
            NotificationEmailDto dto = new NotificationEmailDto(true, "code");
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            service.saveEmailNotification(USER_ID, dto);

            verify(notificationEmailSettings).setNotifyOnAccountDeleted(true);
            verify(notificationEmailSettingsRepository).save(notificationEmailSettings);
            verify(kafkaTemplate).send(eq(TOPIC), any(SettingsActivityEvent.class));
        }
    }
}