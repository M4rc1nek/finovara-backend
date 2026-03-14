package com.finovara.finovarabackend.usersetting.accountsetting.account.service.username;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersetting.account.service.AccountService;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.util.service.user.accountmanagment.usernamepolicy.UsernameChangeEmailService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUsernameTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountChangesActivityService accountChangesActivityService;
    @Mock
    private UsernameChangeEmailService usernameChangeEmailService;
    @Mock
    private HttpServletRequest request;
    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldUpdateUsernameSuccessfully() {

        Long userId = 1L;


        NotificationSettings notificationSettings = new NotificationSettings();
        notificationSettings.setNotifyOnUsernameChange(true);

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setNotificationSettings(notificationSettings);
        AccountSettingsDto accountSettingsDto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(userRepository.existsByUsername(accountSettingsDto.username())).thenReturn(false);

        AccountSettingsDto result = accountService.updateUsername(accountSettingsDto, userId, request);

        assertThat(result.username()).isEqualTo("newUsername");

        verify(userRepository).save(user);
        verify(accountChangesActivityService).createAccountChangesActivity(
                user.getEmail(),
                AccountChangesActivityType.USERNAME_CHANGED,
                request
        );
        verify(usernameChangeEmailService).sendEmail(user);
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {

        Long userId = 1L;


        User user = new User();
        user.setEmail("test@test.com");
        user.setId(userId);

        AccountSettingsDto accountSettingsDto = new AccountSettingsDto("existingUsername", user.getEmail(), null, null);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(userRepository.existsByUsername(accountSettingsDto.username())).thenReturn(true);



        assertThatThrownBy(() -> accountService.updateUsername(accountSettingsDto, userId, request)).isInstanceOf(NameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldNotSendEmailWhenNotificationDisabled() {

        Long userId = 1L;

        NotificationSettings notificationSettings = new NotificationSettings();
        notificationSettings.setNotifyOnUsernameChange(false);

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setNotificationSettings(notificationSettings);

        AccountSettingsDto accountSettingsDto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(userRepository.existsByUsername(accountSettingsDto.username())).thenReturn(false);

        accountService.updateUsername(accountSettingsDto, userId, request);

        verify(userRepository).save(user);
        verify(usernameChangeEmailService, never()).sendEmail(user);
    }
}