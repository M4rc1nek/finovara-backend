package com.finovara.finovarabackend.usersetting.accountsetting.account.service.deleteaccount;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.service.AccountService;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeleteTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordConfirmationService passwordConfirmationService;
    @Mock
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldDeleteAccountSuccessfully() {

        Long userId = 1L;

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

        NotificationEmailSettings notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setNotifyOnAccountDeleted(false);

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setNotificationEmailSettings(notificationEmailSettings);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        accountService.deleteAccount(confirmPasswordDto, userId);

        verify(passwordConfirmationService).confirmPassword(user.getEmail(), confirmPasswordDto);
        verify(userRepository).delete(user);
        verify(notifyOnAccountDeletedService).sendEmail(user);
    }

    @Test
    void shouldSendEmailWhenNotificationEnabled() {

        Long userId = 1L;

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

        NotificationEmailSettings notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setNotifyOnAccountDeleted(true);

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setNotificationEmailSettings(notificationEmailSettings);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        accountService.deleteAccount(confirmPasswordDto, userId);

        verify(userRepository).delete(user);
        verify(notifyOnAccountDeletedService).sendEmail(user);
    }

    @Test
    void shouldConfirmPasswordBeforeDeletingAccount() {

        Long userId = 1L;

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

        NotificationEmailSettings notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setNotifyOnAccountDeleted(false);

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setNotificationEmailSettings(notificationEmailSettings);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        accountService.deleteAccount(confirmPasswordDto, userId);

        verify(passwordConfirmationService).confirmPassword(user.getEmail(), confirmPasswordDto);
    }
}