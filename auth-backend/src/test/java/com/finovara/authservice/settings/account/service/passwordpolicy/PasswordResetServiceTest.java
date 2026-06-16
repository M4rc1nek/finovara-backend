package com.finovara.authservice.settings.account.service.passwordpolicy;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailService;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeManager;
import com.finovara.authservice.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private CredentialValidationService credentialValidationService;
    @Mock
    private VerificationCodeManager verificationCodeManager;
    @Mock
    private VerificationCodeEmailService verificationCodeEmailService;
    @Mock
    private PasswordUpdateService passwordUpdateService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private AccountSettings settings;

    private final String email = "test@mail.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail(email);
        user.setPassword("oldPass");

        settings = new AccountSettings();
        user.setAccountSettings(settings);
    }

    @Nested
    class RequestPasswordReset {
        @Test
        void shouldGenerateAndSendResetCode() {
            PasswordResetRequestDto dto = new PasswordResetRequestDto(email);

            when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
            when(verificationCodeManager.generatePasswordResetCode(settings)).thenReturn(123456);

            passwordResetService.requestPasswordReset(dto);

            verify(verificationCodeManager).generatePasswordResetCode(settings);
            verify(verificationCodeEmailService).sendPasswordResetCode(user, email, 123456);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            PasswordResetRequestDto dto = new PasswordResetRequestDto(email);

            when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> passwordResetService.requestPasswordReset(dto));

            verifyNoInteractions(verificationCodeManager);
            verifyNoInteractions(verificationCodeEmailService);
        }
    }

    @Nested
    class ConfirmPasswordReset {
        private final int code = 123456;

        @Test
        void shouldResetPasswordSuccessfully() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(email, "newPass", "newPass", code);

            AttemptsDto attempts = new AttemptsDto(1, 4, 3);

            when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
            when(verificationCodeManager.getCurrentPasswordResetAttempts(email)).thenReturn(attempts);

            AttemptsDto result = passwordResetService.confirmPasswordReset(dto, request);

            verify(credentialValidationService).validateNewPassword("newPass", "newPass", "oldPass");

            verify(verificationCodeManager).verifyPasswordResetCode(settings, code);
            verify(verificationCodeManager).removePasswordResetCode(settings);

            verify(passwordUpdateService).updatePassword(user, "newPass", request);

            assertEquals(attempts, result);
            assertEquals(1, result.used());
            assertEquals(4, result.max());
            assertEquals(3, result.remaining());
        }

        @Test
        void shouldThrowInvalidVerificationCodeException() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(email, "newPass", "newPass", code);

            AttemptsDto attempts = new AttemptsDto(2, 5, 3);

            when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
            when(verificationCodeManager.verifyPasswordResetAttemptsCode(email, settings)).thenReturn(attempts);

            doThrow(new InvalidInputException("Invalid code")).when(verificationCodeManager).verifyPasswordResetCode(settings, code);

            assertThrows(InvalidVerificationCodeException.class, () -> passwordResetService.confirmPasswordReset(dto, request));

            verify(verificationCodeManager).verifyPasswordResetAttemptsCode(email, settings);

            verify(passwordUpdateService, never()).updatePassword(any(), any(), any());
            verify(verificationCodeManager, never()).removePasswordResetCode(any());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            PasswordResetConfirmDto dto = new PasswordResetConfirmDto(email, "newPass", "newPass", code);

            when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> passwordResetService.confirmPasswordReset(dto, request));

            verifyNoInteractions(credentialValidationService);
            verifyNoInteractions(verificationCodeManager);
        }
    }
}