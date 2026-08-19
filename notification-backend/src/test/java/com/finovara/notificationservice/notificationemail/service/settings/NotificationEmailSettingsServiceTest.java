package com.finovara.notificationservice.notificationemail.service.settings;

import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEmailSettingsServiceTest {

    private static final Long USER_ID = 55L;

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Mock
    private NotificationEmailSettings settings;

    private NotificationEmailSettingsService service;

    @BeforeEach
    void setUp() {
        service = new NotificationEmailSettingsService(notificationEmailSettingsRepository);
    }

    @Nested
    class CreateSettingsIfNotExist {

        @Test
        void shouldSaveSettingsWithAllFlagsDisabledWhenUserIdIsNew() {
            ArgumentCaptor<NotificationEmailSettings> captor = ArgumentCaptor.forClass(NotificationEmailSettings.class);

            service.createSettingsIfNotExist(USER_ID);

            verify(notificationEmailSettingsRepository).save(captor.capture());
            NotificationEmailSettings savedSettings = captor.getValue();
            assertEquals(USER_ID, savedSettings.getUserId());
            assertFalse(savedSettings.isNotifyOnPasswordChange());
            assertFalse(savedSettings.isNotifyOnUsernameChange());
            assertFalse(savedSettings.isNotifyOnEmailChange());
            assertFalse(savedSettings.isNotifyOnAccountDeleted());
        }

        @Test
        void shouldNotThrowExceptionWhenSettingsAlreadyExistForUser() {
            doThrow(new DataIntegrityViolationException("duplicate"))
                    .when(notificationEmailSettingsRepository).save(any(NotificationEmailSettings.class));

            service.createSettingsIfNotExist(USER_ID);

            verify(notificationEmailSettingsRepository).save(any(NotificationEmailSettings.class));
        }
    }

    @Nested
    class DeleteSettings {

        @Test
        void shouldDeleteSettingsWhenDeleteSettingsCalled() {
            service.deleteSettings(settings);

            verify(notificationEmailSettingsRepository).delete(settings);
        }
    }
}