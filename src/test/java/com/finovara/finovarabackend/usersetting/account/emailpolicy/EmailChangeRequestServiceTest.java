package com.finovara.finovarabackend.usersetting.account.emailpolicy;

import com.finovara.finovarabackend.exception.unauthorized.WrongPasswordException;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.emailpolicy.EmailChangeService;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeEmailService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
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
class EmailChangeRequestServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private CredentialValidationService credentialValidationService;

    @Mock
    private VerificationCodeManager verificationCodeManager;

    @Mock
    private VerificationCodeEmailService verificationCodeEmailService;

    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private EmailChangeService emailChangeService;

    private User user;
    private AccountSettings settings;

    private final Long userId = 1L;
    private final String newEmail = "new@mail.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);

        settings = new AccountSettings();
        user.setAccountSettings(settings);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
    }

    @Test
    void shouldGenerateAndSendEmailChangeCode() {
        EmailChangeRequestDto dto = new EmailChangeRequestDto(newEmail, "password123");

        when(verificationCodeManager.generateEmailChangeCode(settings, newEmail)).thenReturn(111111);

        emailChangeService.requestEmailChange(userId, dto);

        verify(credentialValidationService).validateEmailChange(user, newEmail);

        verify(passwordValidator).validatePassword(userId, new ConfirmPasswordDto("password123"));
        verify(verificationCodeManager).generateEmailChangeCode(settings, newEmail);
        verify(verificationCodeEmailService).sendEmailChangeCode(user, newEmail, 111111);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
         EmailChangeRequestDto dto = new EmailChangeRequestDto(newEmail, "password123");

        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> emailChangeService.requestEmailChange(userId, dto));

        verifyNoInteractions(verificationCodeManager);
        verifyNoInteractions(verificationCodeEmailService);
    }
}