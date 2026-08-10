package com.finovara.authservice.settings.account.service.emailpolicy.change;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.authservice.settings.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.service.emailpolicy.attempts.EmailChangeVerificationService;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.email.EmailDomainValidator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

    private static final Long USER_ID = 1L;
    private static final String NEW_EMAIL = "new@test.com";
    private static final String PASSWORD = "password";
    private static final String AUTH_CODE = "1563292";
    private static final int VERIFICATION_CODE = 123456;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private CredentialValidationService credentialValidationService;

    @Mock
    private EmailChangeVerificationService emailChangeVerificationService;

    @Mock
    private VerificationCodeEmailSender verificationCodeEmailSender;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private EmailUpdateService emailUpdateService;

    @Mock
    private EmailDomainValidator emailDomainValidator;

    @Mock
    private AdditionalAuthorizationService additionalAuthorizationService;

    @Mock
    private HttpServletRequest request;

    private EmailChangeService emailChangeService;

    private User user;

    private AccountSettings settings;

    @BeforeEach
    void setUp() {
        emailChangeService = new EmailChangeService(userManagerService, credentialValidationService, emailChangeVerificationService,
                verificationCodeEmailSender, passwordValidator, emailUpdateService, emailDomainValidator, additionalAuthorizationService, new AdditionalAuthorizationCodeResolver());
        user = mock(User.class);
        settings = mock(AccountSettings.class);
    }

    @Nested
    class RequestEmailChange {

        @Test
        void shouldThrowExceptionWhenAdditionalAuthorizationFails() {
            EmailChangeRequestDto dto = new EmailChangeRequestDto(AUTH_CODE, NEW_EMAIL, PASSWORD);
            doThrow(new RuntimeException("invalid authorization"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(eq(USER_ID), any());

            assertThrows(RuntimeException.class, () -> emailChangeService.requestEmailChange(USER_ID, dto));
            verify(userManagerService, never()).getUserByIdOrThrow(any());
        }

        @Test
        void shouldThrowExceptionWhenEmailValidationFails() {
            EmailChangeRequestDto dto = new EmailChangeRequestDto(AUTH_CODE, NEW_EMAIL, PASSWORD);
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            doThrow(new RuntimeException("email already used"))
                    .when(credentialValidationService).validateEmailChange(user, NEW_EMAIL);

            assertThrows(RuntimeException.class, () -> emailChangeService.requestEmailChange(USER_ID, dto));
            verify(verificationCodeEmailSender, never()).sendEmailChangeCode(any(), any(), anyInt());
        }

        @Test
        void shouldNotSendEmailWhenAuthorizationFails() {
            EmailChangeRequestDto dto = new EmailChangeRequestDto(AUTH_CODE, NEW_EMAIL, PASSWORD);
            doThrow(new RuntimeException("invalid authorization"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(eq(USER_ID), any());

            assertThrows(RuntimeException.class, () -> emailChangeService.requestEmailChange(USER_ID, dto));
            verifyNoInteractions(verificationCodeEmailSender);
        }
    }

    @Nested
    class ConfirmEmailChange {

        @Test
        void shouldUpdateEmailWhenCodeAndPasswordAreValid() {
            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(VERIFICATION_CODE, AUTH_CODE);
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            when(settings.getPendingEmail()).thenReturn(NEW_EMAIL);
            when(emailChangeVerificationService.getCurrentAttempts(USER_ID)).thenReturn(new AttemptsDto(0, 5, 5));

            AttemptsDto result = emailChangeService.confirmEmailChange(USER_ID, dto, request);

            assertThat(result.remaining()).isEqualTo(5);
            verify(emailChangeVerificationService).verifyCodeOrThrow(USER_ID, settings, VERIFICATION_CODE);
            verify(emailChangeVerificationService).removeCode(settings);
            verify(emailUpdateService).updateEmail(user, NEW_EMAIL, request);
        }

        @Test
        void shouldThrowExceptionWhenVerificationFails() {
            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(VERIFICATION_CODE, AUTH_CODE);
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            doThrow(new RuntimeException("invalid code"))
                    .when(emailChangeVerificationService)
                    .verifyCodeOrThrow(USER_ID, settings, VERIFICATION_CODE);

            assertThrows(RuntimeException.class, () -> emailChangeService.confirmEmailChange(USER_ID, dto, request));
            verify(emailUpdateService, never()).updateEmail(any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenAdditionalAuthorizationFails() {
            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(VERIFICATION_CODE, AUTH_CODE);
            doThrow(new RuntimeException("invalid authorization"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(eq(USER_ID), any());

            assertThrows(RuntimeException.class, () -> emailChangeService.confirmEmailChange(USER_ID, dto, request));
            verify(userManagerService, never()).getUserByIdOrThrow(any());
        }

        @Test
        void shouldNotUpdateEmailWhenVerificationFails() {
            EmailChangeConfirmDto dto = new EmailChangeConfirmDto(VERIFICATION_CODE, AUTH_CODE);
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(user.getAccountSettings()).thenReturn(settings);
            doThrow(new RuntimeException("invalid code"))
                    .when(emailChangeVerificationService)
                    .verifyCodeOrThrow(USER_ID, settings, VERIFICATION_CODE);

            assertThrows(RuntimeException.class, () -> emailChangeService.confirmEmailChange(USER_ID, dto, request));
            verify(emailUpdateService, never()).updateEmail(any(), any(), any());
        }
    }
}