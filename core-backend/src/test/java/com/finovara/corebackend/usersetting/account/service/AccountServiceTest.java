package com.finovara.corebackend.usersetting.account.service;

import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import com.finovara.corebackend.usersetting.account.dto.AccountSettingsDto;
import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.corebackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
    private KafkaTemplate<String, Object> kafkaTemplate;
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

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");

            AccountSettingsDto dto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(userRepository.existsByUsername(dto.username())).thenReturn(false);

            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");

            AccountSettingsDto result = accountService.updateUsername(dto, userId, request);

            assertThat(result.username()).isEqualTo("newUsername");

            verify(userRepository).save(user);
            ArgumentCaptor<AccountChangesActivityEvent> eventCaptor = ArgumentCaptor.forClass(AccountChangesActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.account-changes"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().type()).isEqualTo(AccountChangesActivityType.USERNAME_CHANGED);
            verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
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

            assertThatThrownBy(() -> accountService.updateUsername(dto, userId, request)).isInstanceOf(EntityAlreadyExistsException.class);

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

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            accountService.deleteAccount(dto, userId);

            verify(passwordValidator).validatePassword(userId, dto);
            verify(userRepository).delete(user);
            verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
        }

        @Test
        void shouldSendEmailWhenNotificationEnabled() {
            Long userId = 1L;

            ConfirmPasswordDto dto = new ConfirmPasswordDto("password");

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            accountService.deleteAccount(dto, userId);

            verify(userRepository).delete(user);
            verify(kafkaTemplate).send(eq("notification.email.send"), any(SendEmailEvent.class));
        }

        @Test
        void shouldConfirmPasswordBeforeDeletingAccount() {
            Long userId = 1L;

            ConfirmPasswordDto dto = new ConfirmPasswordDto("password");

            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            accountService.deleteAccount(dto, userId);

            verify(passwordValidator).validatePassword(userId, dto);
        }
    }
}
