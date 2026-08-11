package com.finovara.authservice.settings.account.service.passwordpolicy;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.service.passwordpolicy.attempts.PasswordResetVerificationService;
import com.finovara.authservice.settings.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String NEW_PASSWORD = "newPassword1!";
    private static final String CONFIRM_NEW_PASSWORD = "newPassword1!";
    private static final String CURRENT_PASSWORD_HASH = "hashedPassword";
    private static final int GENERATED_CODE = 111111;
    private static final int VERIFICATION_CODE = 222222;

    @Mock
    private CredentialValidationService credentialValidationService;

    @Mock
    private PasswordResetVerificationService passwordResetVerificationService;

    @Mock
    private VerificationCodeEmailSender verificationCodeEmailSender;

    @Mock
    private PasswordUpdateService passwordUpdateService;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private HttpServletRequest request;

    private PasswordResetService passwordResetService;

    private User user;

    private AccountSettings settings;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(credentialValidationService, passwordResetVerificationService,
                verificationCodeEmailSender, passwordUpdateService, userManagerService);
        user = mock(User.class);
        settings = mock(AccountSettings.class);
    }

    @Nested
    class RequestPasswordReset {

        @Test
        void shouldGenerateAndSendCodeWhenUserExists() {
            PasswordResetRequestDto dto = new PasswordResetRequestDto(EMAIL);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            when(passwordResetVerificationService.generateCode(settings)).thenReturn(GENERATED_CODE);

            passwordResetService.requestPasswordReset(dto);

            verify(verificationCodeEmailSender).sendPasswordResetCode(user, EMAIL, GENERATED_CODE);
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            PasswordResetRequestDto dto = new PasswordResetRequestDto(EMAIL);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new RuntimeException("user not found"));

            assertThrows(RuntimeException.class, () -> passwordResetService.requestPasswordReset(dto));
            verify(verificationCodeEmailSender, never()).sendPasswordResetCode(any(), any(), any(Integer.class));
        }

        @Test
        void shouldNotSendEmailWhenUserNotFound() {
            PasswordResetRequestDto dto = new PasswordResetRequestDto(EMAIL);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new RuntimeException("user not found"));

            assertThrows(RuntimeException.class, () -> passwordResetService.requestPasswordReset(dto));
            verifyNoInteractions(verificationCodeEmailSender);
        }
    }

    @Nested
    class ConfirmPasswordReset {

        @Test
        void shouldUpdatePasswordWhenCodeAndPasswordAreValid() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(EMAIL, NEW_PASSWORD, CONFIRM_NEW_PASSWORD, VERIFICATION_CODE);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenReturn(new AttemptsDto(0, 5, 5));

            AttemptsDto result = passwordResetService.confirmPasswordReset(dto, request);

            assertThat(result.remaining()).isEqualTo(5);
            verify(credentialValidationService).validateNewPassword(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, CURRENT_PASSWORD_HASH);
            verify(passwordResetVerificationService).verifyCodeOrThrow(EMAIL, settings, VERIFICATION_CODE);
            verify(passwordResetVerificationService).removeCode(settings);
            verify(passwordUpdateService).updatePassword(user, NEW_PASSWORD, request);
        }

        @Test
        void shouldThrowExceptionWhenNewPasswordValidationFails() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(EMAIL, "short", "short", 2342516);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new RuntimeException("password too short"))
                    .when(credentialValidationService)
                    .validateNewPassword("short", "short", CURRENT_PASSWORD_HASH);

            assertThrows(RuntimeException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verify(passwordResetVerificationService, never()).verifyCodeOrThrow(any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenCodeVerificationFails() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(EMAIL,  NEW_PASSWORD, CONFIRM_NEW_PASSWORD, null);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new RuntimeException("invalid code"))
                    .when(passwordResetVerificationService)
                    .verifyCodeOrThrow(EMAIL, settings, VERIFICATION_CODE);

            assertThrows(RuntimeException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verify(passwordUpdateService, never()).updatePassword(any(), any(), any());
        }

        @Test
        void shouldNotUpdatePasswordWhenValidationFails() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(EMAIL, "short", "short", 2342516);
            when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new RuntimeException("password too short"))
                    .when(credentialValidationService)
                    .validateNewPassword("short", "short", CURRENT_PASSWORD_HASH);

            assertThrows(RuntimeException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verify(passwordUpdateService, never()).updatePassword(any(), any(), any());
        }
    }
}