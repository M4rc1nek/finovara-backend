package com.finovara.authservice.settings.account.service;

import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.settings.account.dto.AccountSettingsDto;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxService outboxService;
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

            AccountSettingsDto result = accountService.updateUsername(dto, userId, request);

            assertThat(result.username()).isEqualTo("newUsername");
            verify(userRepository).save(user);
        }

        @Test
        void shouldSaveActivityEventToOutbox() {
            Long userId = 1L;
            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");
            AccountSettingsDto dto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(userRepository.existsByUsername(dto.username())).thenReturn(false);

            accountService.updateUsername(dto, userId, request);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("User"),
                    eq(userId.toString()),
                    eq("activity.account-changes"),
                    payloadCaptor.capture()
            );

            AccountChangesActivityEvent event = (AccountChangesActivityEvent) payloadCaptor.getValue();
            assertThat(event.userId()).isEqualTo(userId);
            assertThat(event.type()).isEqualTo(AccountChangesActivityType.USERNAME_CHANGED);
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        void shouldSaveEmailNotificationToOutbox() {
            Long userId = 1L;
            User user = new User();
            user.setId(userId);
            user.setEmail("test@test.com");
            AccountSettingsDto dto = new AccountSettingsDto("newUsername", user.getEmail(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(userRepository.existsByUsername(dto.username())).thenReturn(false);

            accountService.updateUsername(dto, userId, request);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("User"),
                    eq(userId.toString()),
                    eq("notification.email.send"),
                    payloadCaptor.capture()
            );

            SendEmailEvent event = (SendEmailEvent) payloadCaptor.getValue();
            assertThat(event.userId()).isEqualTo(userId);
            assertThat(event.email()).isEqualTo("test@test.com");
        }

        @Test
        void shouldThrowWhenUsernameAlreadyExists() {
            Long userId = 1L;
            User user = new User();
            user.setId(userId);
            AccountSettingsDto dto = new AccountSettingsDto("existingUsername", "test@test.com", null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(userRepository.existsByUsername(dto.username())).thenReturn(true);

            assertThatThrownBy(() -> accountService.updateUsername(dto, userId, request))
                    .isInstanceOf(EntityAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
            verify(outboxService, never()).save(any(), any(), any(), any());
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
            user.setProfileImagePath(null);

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
}