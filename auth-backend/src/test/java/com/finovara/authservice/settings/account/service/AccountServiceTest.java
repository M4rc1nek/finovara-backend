package com.finovara.authservice.settings.account.service;

import com.finovara.authservice.settings.account.dto.AccountSettingsDto;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.notification.event.SendEmailEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private AdditionalAuthorizationService additionalAuthorizationService;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private AccountSettingsDto testDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@test.com");
        testUser.setCreatedAt(LocalDateTime.of(2024, 1, 10, 12, 0));
        testUser.setProfileImagePath(null);

        testDto = new AccountSettingsDto("testUser", "test@test.com", LocalDateTime.of(2024, 1, 10, 12, 0), null, null);
    }

    @Nested
    class UpdateUsername {

        @Test
        void shouldUpdateUsernameSuccessfully() {
            Long userId = 1L;
            AccountSettingsDto dto = new AccountSettingsDto("newUsername", testUser.getEmail(), LocalDateTime.now(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            doNothing().when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(eq(userId), any());
            when(userRepository.existsByUsername(dto.username())).thenReturn(false);

            AccountSettingsDto result = accountService.updateUsername(dto, userId, request);

            assertThat(result.username()).isEqualTo("newUsername");
            verify(userRepository).save(testUser);
        }

        @Test
        void shouldSaveActivityEventToOutboxWhenUsernameUpdated() {
            Long userId = 1L;
            AccountSettingsDto dto = new AccountSettingsDto("newUsername", testUser.getEmail(), LocalDateTime.now(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            doNothing().when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(eq(userId), any());
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
        void shouldSaveEmailNotificationToOutboxWhenUsernameUpdated() {
            Long userId = 1L;
            AccountSettingsDto dto = new AccountSettingsDto("newUsername", testUser.getEmail(), LocalDateTime.now(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            doNothing().when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(eq(userId), any());
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
        void shouldThrowEntityAlreadyExistsExceptionWhenUsernameAlreadyExists() {
            Long userId = 1L;
            AccountSettingsDto dto = new AccountSettingsDto("existingUsername", testUser.getEmail(), LocalDateTime.now(), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            doNothing().when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(eq(userId), any());
            when(userRepository.existsByUsername(dto.username())).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> accountService.updateUsername(dto, userId, request));

            verify(userRepository, never()).save(any());
            verify(outboxService, never()).save(any(), any(), any(), any());
        }
    }

    @Nested
    class GetAccountSettings {

        @Test
        void shouldReturnAccountSettingsWhenUserExists() {
            Long userId = 1L;
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);

            AccountSettingsDto result = accountService.getAccountSettings(userId);

            assertThat(result.username()).isEqualTo("testUser");
            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.createdAt()).isEqualTo(testUser.getCreatedAt());
            assertThat(result.profileImageUrl()).isNull();
        }

        @Test
        void shouldReturnNullProfileImageUrlWhenPathIsNull() {
            Long userId = 1L;
            testUser.setProfileImagePath(null);
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);

            AccountSettingsDto result = accountService.getAccountSettings(userId);

            assertThat(result.profileImageUrl()).isNull();
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenUserNotFound() {
            Long userId = 999L;
            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> accountService.getAccountSettings(userId));
        }
    }
}