package com.finovara.notificationservice.notificationemail.service.settings.action.core;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractActionNotificationEmailServiceTest {

    private static final Long USER_ID = 1L;
    private static final String AUTH_CODE = "AUTH123";

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private NotificationEmailSettings notificationEmailSettings;

    private TestActionNotificationEmailService service;

    static class TestActionNotificationEmailService extends AbstractActionNotificationEmailService {

        TestActionNotificationEmailService(NotificationEmailSettingsRepository repository,
                                           AuthBackendClient authBackendClient,
                                           AdditionalAuthorizationCodeResolver resolver) {
            super(repository, authBackendClient, resolver);
        }

        @Override
        protected boolean isEnabled(NotificationEmailDto dto) {
            return dto.enabled();
        }

        @Override
        protected void applySetting(NotificationEmailSettings settings, boolean value) {
            settings.setNotifyOnUsernameChange(value);
        }

        @Override
        protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
            return new NotificationEmailDto(settings.isNotifyOnUsernameChange(), null);
        }
    }

    @BeforeEach
    void setUp() {
        service = new TestActionNotificationEmailService(notificationEmailSettingsRepository, authBackendClient, additionalAuthorizationCodeResolver);
    }

    @Nested
    class SaveEmailNotification {

        @Test
        void shouldConfirmAuthorizationCodeWhenSaving() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            service.saveEmailNotification(USER_ID, dto);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
        }

        @Test
        void shouldEnableSettingWhenDtoEnabledIsTrue() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            service.saveEmailNotification(USER_ID, dto);

            verify(notificationEmailSettings).setNotifyOnUsernameChange(true);
        }

        @Test
        void shouldDisableSettingWhenDtoEnabledIsFalse() {
            NotificationEmailDto dto = new NotificationEmailDto(false, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            service.saveEmailNotification(USER_ID, dto);

            verify(notificationEmailSettings).setNotifyOnUsernameChange(false);
        }

        @Test
        void shouldPersistSettingsAfterApplyingChange() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));

            service.saveEmailNotification(USER_ID, dto);

            verify(notificationEmailSettingsRepository).save(notificationEmailSettings);
        }

        @Test
        void shouldThrowExceptionWhenSettingsNotFound() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.saveEmailNotification(USER_ID, dto));
        }

        @Test
        void shouldNotSaveSettingsWhenNotFound() {
            NotificationEmailDto dto = new NotificationEmailDto(true, AUTH_CODE);

            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.saveEmailNotification(USER_ID, dto));

            verify(notificationEmailSettingsRepository, never()).save(any());
        }
    }

    @Nested
    class GetEmailNotification {

        @Test
        void shouldReturnDtoWhenSettingsExist() {
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));
            when(notificationEmailSettings.isNotifyOnUsernameChange()).thenReturn(true);

            NotificationEmailDto result = service.getEmailNotification(USER_ID);

            assertTrue(result.enabled());
        }

        @Test
        void shouldReturnDisabledDtoWhenSettingIsFalse() {
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(notificationEmailSettings));
            when(notificationEmailSettings.isNotifyOnUsernameChange()).thenReturn(false);

            NotificationEmailDto result = service.getEmailNotification(USER_ID);

            assertFalse(result.enabled());
        }

        @Test
        void shouldThrowExceptionWhenSettingsNotFound() {
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getEmailNotification(USER_ID));
        }
    }
}