package com.finovara.finovarabackend.usersetting.account.passwordpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.AttemptsDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.PasswordResetService;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetConfirmServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private CredentialValidationService credentialValidationService;
    @Mock
    private VerificationCodeManager verificationCodeManager;
    @Mock
    private PasswordUpdateService passwordUpdateService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private AccountSettings settings;

    private final String email = "test@mail.com";

    private PasswordResetConfirmDto validDto;
    private PasswordResetConfirmDto invalidConfirmDto;
    private PasswordResetConfirmDto wrongCodeDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setPassword("oldPass");

        settings = new AccountSettings();
        user.setAccountSettings(settings);

        validDto = new PasswordResetConfirmDto(email, "newPass", "newPass", 123456);
        invalidConfirmDto = new PasswordResetConfirmDto(email, "newPass", "wrongConfirm", 123456);
        wrongCodeDto = new PasswordResetConfirmDto(email, "newPass", "newPass", 123456);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
    }

    @Test
    void shouldResetPasswordSuccessfully() {
        AttemptsDto attemptsDto = new AttemptsDto(1, 4, 3);

        when(verificationCodeManager.getCurrentPasswordResetAttempts(email)).thenReturn(attemptsDto);

        AttemptsDto result = passwordResetService.confirmPasswordReset(validDto, request);

        verify(credentialValidationService).validateNewPassword("newPass", "newPass", "oldPass");

        verify(verificationCodeManager).verifyPasswordResetCode(settings, 123456);
        verify(verificationCodeManager).removePasswordResetCode(settings);

        verify(passwordUpdateService).updatePassword(user, "newPass", request);

        assertEquals(attemptsDto, result);

        assertEquals(1, result.used());
        assertEquals(4, result.max());
        assertEquals(3, result.remaining());
    }

    @Test
    void shouldThrowInvalidVerificationCodeException() {
        AttemptsDto attemptsDto = new AttemptsDto(2, 5, 3);

        when(verificationCodeManager.verifyPasswordResetAttemptsCode(email, settings)).thenReturn(attemptsDto);

        doThrow(new InvalidInputException("Invalid code")).when(verificationCodeManager).verifyPasswordResetCode(settings, 123456);

        assertThrows(InvalidVerificationCodeException.class, () -> passwordResetService.confirmPasswordReset(wrongCodeDto, request));

        verify(verificationCodeManager).verifyPasswordResetAttemptsCode(email, settings);

        verify(passwordUpdateService, never()).updatePassword(any(), any(), any());

        verify(verificationCodeManager, never()).removePasswordResetCode(any());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> passwordResetService.confirmPasswordReset(validDto, request));

        verifyNoInteractions(credentialValidationService);
    }
}