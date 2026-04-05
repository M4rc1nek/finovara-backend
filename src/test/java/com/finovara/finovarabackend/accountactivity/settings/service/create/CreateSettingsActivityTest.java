package com.finovara.finovarabackend.accountactivity.settings.service.create;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSettingsActivityTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private SettingsActivityRepository settingsActivityRepository;
    @InjectMocks
    private SettingsActivityService settingsActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldCreateSettingsActivitySuccessfully() {

        User user = new User();
        user.setId(1L);
        LocalDateTime now = LocalDateTime.now();

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        settingsActivityService.createSettingActivity(
                EMAIL,
                SettingActivityStatus.ENABLED,
                SettingType.NOTIFICATION_PASSWORD_CHANGED
        );

        verify(settingsActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getStatus() == SettingActivityStatus.ENABLED &&
                        activity.getSettingType() == SettingType.NOTIFICATION_PASSWORD_CHANGED &&
                        !activity.getDate().isBefore(now)
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                settingsActivityService.createSettingActivity(
                        EMAIL,
                        SettingActivityStatus.ENABLED,
                        SettingType.PIGGY_BANK_ROUND_UP
                )
        );

        verify(settingsActivityRepository, never()).save(any());
    }
}