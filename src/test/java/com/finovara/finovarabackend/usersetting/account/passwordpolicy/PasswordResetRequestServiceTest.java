package com.finovara.finovarabackend.usersetting.account.passwordpolicy;

import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.PasswordResetService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeEmailService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetRequestServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private VerificationCodeManager verificationCodeManager;
    @Mock
    private VerificationCodeEmailService verificationCodeEmailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private AccountSettings settings;

    private final String email = "test@mail.com";

    @BeforeEach
    void setUp() {
        user = new User();
        settings = new AccountSettings();
        user.setAccountSettings(settings);
    }

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
    void shouldThrowExceptionWhenUserNotFound() {
        PasswordResetRequestDto dto = new PasswordResetRequestDto(email);

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> passwordResetService.requestPasswordReset(dto));

        verifyNoInteractions(verificationCodeManager);
        verifyNoInteractions(verificationCodeEmailService);
    }
}