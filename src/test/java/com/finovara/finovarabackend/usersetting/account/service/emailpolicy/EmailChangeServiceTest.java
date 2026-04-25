package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.AttemptsDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeEmailService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

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
    @Mock
    private EmailUpdateService emailUpdateService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EmailChangeService emailChangeService;

    private User user;
    private AccountSettings settings;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        settings = new AccountSettings();
        user = new User();
        user.setId(userId);
        user.setAccountSettings(settings);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
    }

    @Nested
    class RequestEmailChange {

        @Test
        void shouldRequestEmailChangeSuccessfully() {
            String email = "new@mail.com";
            String password = "password";
            int code = 123456;

            when(verificationCodeManager.generateEmailChangeCode(settings, email)).thenReturn(code);

            EmailChangeRequestDto dto = new EmailChangeRequestDto(email, password);

            emailChangeService.requestEmailChange(userId, dto);

            verify(credentialValidationService).validateEmailChange(user, email);
            verify(passwordValidator).validatePassword(eq(userId), any());
            verify(verificationCodeManager).generateEmailChangeCode(settings, email);
            verify(verificationCodeEmailService).sendEmailChangeCode(user, email, code);
        }

        @Test
        void shouldNotGenerateCodeWhenValidationFails() {
            String email = "bad@mail.com";
            String password = "password";

            doThrow(new InvalidInputException("invalid")).when(credentialValidationService).validateEmailChange(user, email);

            EmailChangeRequestDto dto = new EmailChangeRequestDto(email, password);

            assertThatThrownBy(() -> emailChangeService.requestEmailChange(userId, dto)).isInstanceOf(InvalidInputException.class);

            verify(verificationCodeManager, never()).generateEmailChangeCode(any(), any());
            verify(verificationCodeEmailService, never()).sendEmailChangeCode(any(), any(), anyInt());
        }

        @Test
        void shouldNotValidatePasswordIfEmailValidationFails() {
            String email = "bad@mail.com";
            String password = "password";

            doThrow(new InvalidInputException("email invalid")).when(credentialValidationService).validateEmailChange(user, email);

            EmailChangeRequestDto dto = new EmailChangeRequestDto(email, password);

            assertThatThrownBy(() -> emailChangeService.requestEmailChange(userId, dto)).isInstanceOf(InvalidInputException.class);

            verify(passwordValidator, never()).validatePassword(anyLong(), any());
        }
    }

    @Nested
    class ConfirmEmailChange {
        private final int code = 123456;
        private final String newEmail = "new@mail.com";

        @BeforeEach
        void setUp() {
            settings.setPendingEmail(newEmail);
        }

        @Test
        void shouldConfirmEmailChangeSuccessfully() {
            AttemptsDto attempts = new AttemptsDto(1, 5, 4);

            when(verificationCodeManager.getCurrentEmailChangeAttempts(userId)).thenReturn(attempts);

            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(code);

            AttemptsDto result = emailChangeService.confirmEmailChange(userId, dto, request);

            assertThat(result).isEqualTo(attempts);

            verify(verificationCodeManager).verifyEmailChangeCode(settings, code);
            verify(verificationCodeManager).removeEmailChangeCode(settings);
            verify(emailUpdateService).updateEmail(user, newEmail, request);
        }

        @Test
        void shouldNotRemoveCodeWhenVerificationFails() {
            doThrow(new InvalidInputException("Invalid code")).when(verificationCodeManager).verifyEmailChangeCode(settings, code);

            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(code);

            assertThatThrownBy(() -> emailChangeService.confirmEmailChange(userId, dto, request)).isInstanceOf(InvalidVerificationCodeException.class);

            verify(verificationCodeManager, never()).removeEmailChangeCode(any());
            verify(emailUpdateService, never()).updateEmail(any(), any(), any());
        }

        @Test
        void shouldUpdateEmailWithCorrectPendingEmail() {
            AttemptsDto attempts = new AttemptsDto(2, 5, 3);
            String anotherEmail = "different@mail.com";

            settings.setPendingEmail(anotherEmail);

            when(verificationCodeManager.getCurrentEmailChangeAttempts(userId)).thenReturn(attempts);

            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(code);

            emailChangeService.confirmEmailChange(userId, dto, request);

            verify(emailUpdateService).updateEmail(user, anotherEmail, request);
        }
    }
}