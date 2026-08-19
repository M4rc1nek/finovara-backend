package com.finovara.notificationservice.notificationemail.service.settings.action.usernamechange.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyUsernameChangeServiceActionTest {

    private static final Long USER_ID = 1L;
    private static final String AUTH_CODE = "AUTH123";

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

    private NotifyUsernameChangeServiceAction action;

    @BeforeEach
    void setUp() {
        action = new NotifyUsernameChangeServiceAction(notificationEmailSettingsRepository, authBackendClient, kafkaTemplate, additionalAuthorizationCodeResolver);
    }

    @Nested
    class IsEnabled {

        @Test
        void shouldReturnTrueWhenDtoEnabledIsTrue() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            assertTrue(action.isEnabled(dto));
        }

        @Test
        void shouldReturnFalseWhenDtoEnabledIsFalse() {
            NotificationEmailDto dto = new NotificationEmailDto(false, AUTH_CODE);

            assertFalse(action.isEnabled(dto));
        }
    }

    @Nested
    class ApplySetting {

        @Test
        void shouldSetNotifyOnUsernameChangeToTrue() {
            action.applySetting(notificationEmailSettings, true);

            verify(notificationEmailSettings).setNotifyOnUsernameChange(true);
        }

        @Test
        void shouldSetNotifyOnUsernameChangeToFalse() {
            action.applySetting(notificationEmailSettings, false);

            verify(notificationEmailSettings).setNotifyOnUsernameChange(false);
        }
    }

    @Nested
    class MapToDto {

        @Test
        void shouldMapEnabledTrueFromSettings() {
            when(notificationEmailSettings.isNotifyOnUsernameChange()).thenReturn(true);

            NotificationEmailDto dto = action.mapToDto(notificationEmailSettings);

            assertTrue(dto.enabled());
        }

        @Test
        void shouldMapEnabledFalseFromSettings() {
            when(notificationEmailSettings.isNotifyOnUsernameChange()).thenReturn(false);

            NotificationEmailDto dto = action.mapToDto(notificationEmailSettings);

            assertFalse(dto.enabled());
        }
    }

    @Nested
    class HandleActivity {

        @Test
        void shouldSendKafkaEventWhenHandlingActivity() {
            action.handleActivity(USER_ID, true);

            verify(kafkaTemplate).send(eq("activity.settings"), any());
        }

        @Test
        void shouldSendKafkaEventWhenDisabled() {
            action.handleActivity(USER_ID, false);

            verify(kafkaTemplate).send(eq("activity.settings"), any());
        }
    }

    @Nested
    class SaveEmailNotificationIntegration {

        @Test
        void shouldApplySettingAndTriggerKafkaEventWhenSaving() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            action.saveEmailNotification(USER_ID, dto);

            verify(notificationEmailSettings).setNotifyOnUsernameChange(true);
            verify(notificationEmailSettingsRepository).save(notificationEmailSettings);
            verify(kafkaTemplate).send(eq("activity.settings"), any());
        }

        @Test
        void shouldNotSendKafkaEventWhenSettingsNotFound() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            try {
                action.saveEmailNotification(USER_ID, dto);
            } catch (RuntimeException ignored) {
            }

            verify(kafkaTemplate, never()).send(any(), any());
        }
    }
}