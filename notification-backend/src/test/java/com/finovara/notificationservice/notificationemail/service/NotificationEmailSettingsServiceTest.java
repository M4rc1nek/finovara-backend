package com.finovara.notificationservice.notificationemail.service;

import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEmailSettingsServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @InjectMocks
    private NotificationEmailSettingsService notificationEmailSettingsService;

    @Nested
    class CreateSettingsIfNotExist {

        @Test
        void shouldSaveSettingsWithAllNotificationsDisabledWhenUserDoesNotExist() {
            notificationEmailSettingsService.createSettingsIfNotExist(USER_ID);

            ArgumentCaptor<NotificationEmailSettings> captor = ArgumentCaptor.forClass(NotificationEmailSettings.class);
            verify(notificationEmailSettingsRepository).save(captor.capture());

            NotificationEmailSettings settings = captor.getValue();
            assertThat(settings.getUserId()).isEqualTo(USER_ID);
            assertThat(settings.isNotifyOnPasswordChange()).isFalse();
            assertThat(settings.isNotifyOnUsernameChange()).isFalse();
            assertThat(settings.isNotifyOnEmailChange()).isFalse();
            assertThat(settings.isNotifyOnAccountDeleted()).isFalse();
        }

        @Test
        void shouldNotThrowWhenSettingsAlreadyExist() {
            when(notificationEmailSettingsRepository.save(any(NotificationEmailSettings.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            assertThatCode(() -> notificationEmailSettingsService.createSettingsIfNotExist(USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCallSaveOnlyOnceWhenDuplicateKeyThrown() {
            when(notificationEmailSettingsRepository.save(any(NotificationEmailSettings.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            notificationEmailSettingsService.createSettingsIfNotExist(USER_ID);

            verify(notificationEmailSettingsRepository, times(1)).save(any(NotificationEmailSettings.class));
        }
    }

    @Nested
    class DeleteSettings {

        @Test
        void shouldCallRepositoryDelete() {
            NotificationEmailSettings settings = NotificationEmailSettings.builder().userId(USER_ID).build();

            notificationEmailSettingsService.deleteSettings(settings);

            verify(notificationEmailSettingsRepository).delete(settings);
        }
    }
}
