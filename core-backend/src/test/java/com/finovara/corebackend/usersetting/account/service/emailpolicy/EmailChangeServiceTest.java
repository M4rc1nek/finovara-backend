package com.finovara.corebackend.usersetting.account.service.emailpolicy;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.account.dto.AttemptsDto;
import com.finovara.corebackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.corebackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.corebackend.usersetting.account.model.AccountSettings;
import com.finovara.corebackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.corebackend.usersetting.account.service.verification.VerificationCodeEmailService;
import com.finovara.corebackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.corebackend.util.email.EmailDomainValidator;
import com.finovara.corebackend.util.user.service.UserManagerService;
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
    private EmailDomainValidator emailDomainValidator;
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

            emailChangeService.requestEmailChange(userId, new EmailChangeRequestDto(email, password));

            verify(credentialValidationService).validateEmailChange(user, email);
            verify(emailDomainValidator).validateDomainHasMxRecord(email);
            verify(passwordValidator).validatePassword(userId, new ConfirmPasswordDto(password));
            verify(verificationCodeManager).generateEmailChangeCode(settings, email);
            verify(verificationCodeEmailService).sendEmailChangeCode(user, email, code);
        }

        @Test
        void shouldNotGenerateCodeWhenEmailValidationFails() {
            String email = "bad@mail.com";
            String password = "password";

            doThrow(new InvalidInputException("invalid"))
                    .when(credentialValidationService).validateEmailChange(user, email);

            assertThatThrownBy(() -> emailChangeService.requestEmailChange(userId, new EmailChangeRequestDto(email, password)))
                    .isInstanceOf(InvalidInputException.class);

            verify(emailDomainValidator, never()).validateDomainHasMxRecord(any());
            verify(passwordValidator, never()).validatePassword(anyLong(), any());
            verify(verificationCodeManager, never()).generateEmailChangeCode(any(), any());
            verify(verificationCodeEmailService, never()).sendEmailChangeCode(any(), any(), anyInt());
        }

        @Test
        void shouldNotValidatePasswordIfDomainValidationFails() {
            String email = "bad@mail.com";
            String password = "password";

            doThrow(new InvalidInputException("invalid domain"))
                    .when(emailDomainValidator).validateDomainHasMxRecord(email);

            assertThatThrownBy(() -> emailChangeService.requestEmailChange(userId, new EmailChangeRequestDto(email, password)))
                    .isInstanceOf(InvalidInputException.class);

            verify(passwordValidator, never()).validatePassword(anyLong(), any());
            verify(verificationCodeManager, never()).generateEmailChangeCode(any(), any());
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

            AttemptsDto result = emailChangeService.confirmEmailChange(userId, new EmailChangeConfirmDto(code), request);

            assertThat(result).isEqualTo(attempts);
            verify(verificationCodeManager).verifyEmailChangeCode(settings, code);
            verify(verificationCodeManager).removeEmailChangeCode(settings);
            verify(emailUpdateService).updateEmail(user, newEmail, request);
        }

        @Test
        void shouldNotRemoveCodeWhenVerificationFails() {
            AttemptsDto attempts = new AttemptsDto(2, 5, 3);

            doThrow(new InvalidInputException("Invalid code"))
                    .when(verificationCodeManager).verifyEmailChangeCode(settings, code);

            when(verificationCodeManager.verifyEmailChangeAttemptsCode(userId, settings)).thenReturn(attempts);

            assertThatThrownBy(() -> emailChangeService.confirmEmailChange(userId, new EmailChangeConfirmDto(code), request))
                    .isInstanceOf(InvalidVerificationCodeException.class);

            verify(verificationCodeManager, never()).removeEmailChangeCode(any());
            verify(emailUpdateService, never()).updateEmail(any(), any(), any());
        }

        @Test
        void shouldUpdateEmailWithCorrectPendingEmail() {
            String anotherEmail = "different@mail.com";
            settings.setPendingEmail(anotherEmail);

            AttemptsDto attempts = new AttemptsDto(2, 5, 3);
            when(verificationCodeManager.getCurrentEmailChangeAttempts(userId)).thenReturn(attempts);

            emailChangeService.confirmEmailChange(userId, new EmailChangeConfirmDto(code), request);

            verify(emailUpdateService).updateEmail(user, anotherEmail, request);
        }
    }
}