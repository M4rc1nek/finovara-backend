package com.finovara.finovarabackend.usersetting.accountsetting.account.service.username;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersetting.account.service.AccountService;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.action.usernamechange.service.NotifyUsernameChangeService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private NotifyUsernameChangeService notifyUsernameChangeService;
    @Mock
    private HttpServletRequest request;
    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldUpdateUsernameSuccessfully() {

        Long userId = 1L;

        NotificationEmailSettings notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setNotifyOnUsernameChange(true);

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setNotificationEmailSettings(notificationEmailSettings);
        AccountSettingsDto accountSettingsDto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(userRepository.existsByUsername(accountSettingsDto.username())).thenReturn(false);

        AccountSettingsDto result = accountService.updateUsername(accountSettingsDto, userId, request);

        assertThat(result.username()).isEqualTo("newUsername");

        verify(userRepository).save(user);
        verify(accountChangesActivityService).createAccountChangesActivity(
                userId,
                AccountChangesActivityType.USERNAME_CHANGED,
                request
        );
        verify(notifyUsernameChangeService).sendEmail(user);
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
}