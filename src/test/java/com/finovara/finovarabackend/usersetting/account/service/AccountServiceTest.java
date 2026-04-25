package com.finovara.finovarabackend.usersetting.account.service;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.finovarabackend.usersetting.notificationemail.action.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.finovarabackend.usersetting.notificationemail.action.usernamechange.service.NotifyUsernameChangeService;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountChangesActivityService accountChangesActivityService;
    @Mock
    private NotifyUsernameChangeService notifyUsernameChangeService;
    @Mock
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;
    @Mock
    private PasswordValidator passwordValidator;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AccountService accountService;

    @Nested
    class UpdateUsername {
        @Test
        void shouldUpdateUsernameSuccessfully() {
            Long userId = 1L;

            NotificationEmailSettings notificationEmailSettings = new NotificationEmailSettings();
            notificationEmailSettings.setNotifyOnUsernameChange(true);

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");
            user.setNotificationEmailSettings(notificationEmailSettings);

            AccountSettingsDto dto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(userRepository.existsByUsername(dto.username())).thenReturn(false);

            AccountSettingsDto result = accountService.updateUsername(dto, userId, request);

            assertThat(result.username()).isEqualTo("newUsername");

            verify(userRepository).save(user);
            verify(accountChangesActivityService).createAccountChangesActivity(userId, AccountChangesActivityType.USERNAME_CHANGED, request);
            verify(notifyUsernameChangeService).sendEmail(user);
        }

        @Test
        void shouldThrowWhenUsernameAlreadyExists() {
            Long userId = 1L;

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");

            AccountSettingsDto dto = new AccountSettingsDto("existingUsername", user.getEmail(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(userRepository.existsByUsername(dto.username())).thenReturn(true);

            assertThatThrownBy(() -> accountService.updateUsername(dto, userId, request)).isInstanceOf(NameAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class GetAccountSettings {
        @Test
        void shouldReturnAccountSettings() {
            Long userId = 1L;

            User user = new User();
            user.setUsername("john123");
            user.setEmail("test@test.com");
            user.setCreatedAt(LocalDateTime.of(2024, 1, 10, 12, 0));
            user.setProfileImagePath("avatar.png");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            AccountSettingsDto result = accountService.getAccountSettings(userId);

            assertThat(result.username()).isEqualTo("john123");
            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.createdAt()).isEqualTo(user.getCreatedAt());
        }

        @Test
        void shouldReturnNullProfileImageWhenPathIsNull() {
            Long userId = 1L;

            User user = new User();
            user.setUsername("john123");
            user.setEmail("test@test.com");
            user.setCreatedAt(LocalDateTime.of(2026, 3, 1, 12, 0));
            user.setProfileImagePath(null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            AccountSettingsDto result = accountService.getAccountSettings(userId);

            assertThat(result.profileImageUrl()).isNull();
        }
    }

    @Nested
    class DeleteAccount {
        @Test
        void shouldDeleteAccountSuccessfully() {
            Long userId = 1L;

            ConfirmPasswordDto dto = new ConfirmPasswordDto("password");

            NotificationEmailSettings settings = new NotificationEmailSettings();
            settings.setNotifyOnAccountDeleted(false);

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");
            user.setNotificationEmailSettings(settings);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            accountService.deleteAccount(dto, userId);

            verify(passwordValidator).validatePassword(userId, dto);
            verify(userRepository).delete(user);
            verify(notifyOnAccountDeletedService).sendEmail(user);
        }

        @Test
        void shouldSendEmailWhenNotificationEnabled() {
            Long userId = 1L;

            ConfirmPasswordDto dto = new ConfirmPasswordDto("password");

            NotificationEmailSettings settings = new NotificationEmailSettings();
            settings.setNotifyOnAccountDeleted(true);

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");
            user.setNotificationEmailSettings(settings);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            accountService.deleteAccount(dto, userId);

            verify(userRepository).delete(user);
            verify(notifyOnAccountDeletedService).sendEmail(user);
        }

        @Test
        void shouldConfirmPasswordBeforeDeletingAccount() {
            Long userId = 1L;

            ConfirmPasswordDto dto = new ConfirmPasswordDto("password");

            NotificationEmailSettings settings = new NotificationEmailSettings();
            settings.setNotifyOnAccountDeleted(false);

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");
            user.setNotificationEmailSettings(settings);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            accountService.deleteAccount(dto, userId);

            verify(passwordValidator).validatePassword(userId, dto);
        }
    }
}