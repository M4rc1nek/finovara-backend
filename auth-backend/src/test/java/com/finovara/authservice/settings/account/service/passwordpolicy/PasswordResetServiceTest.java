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
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String STORED_EMAIL = "stored@test.com";
    private static final String NEW_PASSWORD = "newPassword1!";
    private static final String CONFIRM_NEW_PASSWORD = "newPassword1!";
    private static final String DIFFERENT_CONFIRM_PASSWORD = "otherPassword1!";
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
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    private AccountSettings settings;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        settings = mock(AccountSettings.class);
    }

    @Nested
    class RequestPasswordReset {

        private PasswordResetRequestDto dto;

        @BeforeEach
        void setUp() {
            dto = new PasswordResetRequestDto(EMAIL);
        }

        @Test
        void shouldGenerateAndSendCodeWhenUserExists() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getEmail()).thenReturn(EMAIL);
            when(user.getAccountSettings()).thenReturn(settings);
            when(passwordResetVerificationService.generateCode(settings)).thenReturn(GENERATED_CODE);

            passwordResetService.requestPasswordReset(dto);

            verify(passwordResetVerificationService).generateCode(settings);
            verify(verificationCodeEmailSender).sendPasswordResetCode(user, EMAIL, GENERATED_CODE);
        }

        @Test
        void shouldSendCodeToEmailStoredInUserWhenUserExists() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getEmail()).thenReturn(STORED_EMAIL);
            when(user.getAccountSettings()).thenReturn(settings);
            when(passwordResetVerificationService.generateCode(settings)).thenReturn(GENERATED_CODE);

            passwordResetService.requestPasswordReset(dto);

            verify(verificationCodeEmailSender).sendPasswordResetCode(user, STORED_EMAIL, GENERATED_CODE);
        }

        @Test
        void shouldGenerateCodeBeforeSendingEmailWhenUserExists() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getEmail()).thenReturn(EMAIL);
            when(user.getAccountSettings()).thenReturn(settings);
            when(passwordResetVerificationService.generateCode(settings)).thenReturn(GENERATED_CODE);

            passwordResetService.requestPasswordReset(dto);

            InOrder inOrder = inOrder(passwordResetVerificationService, verificationCodeEmailSender);
            inOrder.verify(passwordResetVerificationService).generateCode(settings);
            inOrder.verify(verificationCodeEmailSender).sendPasswordResetCode(user, EMAIL, GENERATED_CODE);
        }

        @Test
        void shouldDoNothingWhenUserDoesNotExist() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());

            passwordResetService.requestPasswordReset(dto);

            verifyNoInteractions(passwordResetVerificationService, verificationCodeEmailSender);
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(NullPointerException.class, () -> passwordResetService.requestPasswordReset(null));
            verifyNoInteractions(userRepository, passwordResetVerificationService, verificationCodeEmailSender);
        }

        @Test
        void shouldThrowExceptionWhenCodeGenerationFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getEmail()).thenReturn(EMAIL);
            when(user.getAccountSettings()).thenReturn(settings);
            when(passwordResetVerificationService.generateCode(settings)).thenThrow(new IllegalStateException("generation failed"));

            assertThrows(IllegalStateException.class, () -> passwordResetService.requestPasswordReset(dto));
            verifyNoInteractions(verificationCodeEmailSender);
        }

        @Test
        void shouldThrowExceptionWhenEmailSendingFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getEmail()).thenReturn(EMAIL);
            when(user.getAccountSettings()).thenReturn(settings);
            when(passwordResetVerificationService.generateCode(settings)).thenReturn(GENERATED_CODE);
            doThrow(new IllegalStateException("send failed"))
                    .when(verificationCodeEmailSender)
                    .sendPasswordResetCode(user, EMAIL, GENERATED_CODE);

            assertThrows(IllegalStateException.class, () -> passwordResetService.requestPasswordReset(dto));
        }

        @Test
        void shouldThrowExceptionWhenUserLookupFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenThrow(new IllegalStateException("db error"));

            assertThrows(IllegalStateException.class, () -> passwordResetService.requestPasswordReset(dto));
            verifyNoInteractions(passwordResetVerificationService, verificationCodeEmailSender);
        }
    }

    @Nested
    class ConfirmPasswordReset {

        private PasswordResetConfirmDto dto;

        private AttemptsDto attemptsDto;

        @BeforeEach
        void setUp() {
            dto = new PasswordResetConfirmDto(EMAIL, NEW_PASSWORD, CONFIRM_NEW_PASSWORD, VERIFICATION_CODE);
            attemptsDto = new AttemptsDto(0, 5, 5);
        }

        @Test
        void shouldUpdatePasswordWhenCodeAndPasswordAreValid() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenReturn(attemptsDto);

            AttemptsDto result = passwordResetService.confirmPasswordReset(dto, request);

            assertThat(result).isSameAs(attemptsDto);
            verify(credentialValidationService).validateNewPassword(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, CURRENT_PASSWORD_HASH);
            verify(passwordResetVerificationService).verifyCodeOrThrow(EMAIL, settings, VERIFICATION_CODE);
            verify(passwordResetVerificationService).removeCode(settings);
            verify(passwordUpdateService).updatePassword(user, NEW_PASSWORD, request);
        }

        @Test
        void shouldReturnAttemptsReadBeforeCodeRemovalWhenResetSucceeds() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenReturn(attemptsDto);

            passwordResetService.confirmPasswordReset(dto, request);

            InOrder inOrder = inOrder(passwordResetVerificationService);
            inOrder.verify(passwordResetVerificationService).verifyCodeOrThrow(EMAIL, settings, VERIFICATION_CODE);
            inOrder.verify(passwordResetVerificationService).getCurrentAttempts(EMAIL);
            inOrder.verify(passwordResetVerificationService).removeCode(settings);
        }

        @Test
        void shouldValidatePasswordBeforeVerifyingCodeAndUpdatingPasswordWhenDataIsValid() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenReturn(attemptsDto);

            passwordResetService.confirmPasswordReset(dto, request);

            InOrder inOrder = inOrder(credentialValidationService, passwordResetVerificationService, passwordUpdateService);
            inOrder.verify(credentialValidationService).validateNewPassword(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, CURRENT_PASSWORD_HASH);
            inOrder.verify(passwordResetVerificationService).verifyCodeOrThrow(EMAIL, settings, VERIFICATION_CODE);
            inOrder.verify(passwordUpdateService).updatePassword(user, NEW_PASSWORD, request);
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verifyNoInteractions(credentialValidationService, passwordResetVerificationService, passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(NullPointerException.class, () -> passwordResetService.confirmPasswordReset(null, request));
            verifyNoInteractions(userRepository, credentialValidationService, passwordResetVerificationService, passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenNewPasswordValidationFails() {
            PasswordResetConfirmDto invalidDto = new PasswordResetConfirmDto(EMAIL, "short", "short", VERIFICATION_CODE);
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new IllegalArgumentException("password too short"))
                    .when(credentialValidationService)
                    .validateNewPassword("short", "short", CURRENT_PASSWORD_HASH);

            assertThrows(IllegalArgumentException.class, () -> passwordResetService.confirmPasswordReset(invalidDto, request));
            verify(passwordResetVerificationService, never()).verifyCodeOrThrow(eq(EMAIL), eq(settings), eq(VERIFICATION_CODE));
            verify(passwordResetVerificationService, never()).removeCode(settings);
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenPasswordsDoNotMatch() {
            PasswordResetConfirmDto mismatchedDto = new PasswordResetConfirmDto(EMAIL, NEW_PASSWORD, DIFFERENT_CONFIRM_PASSWORD, VERIFICATION_CODE);
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new IllegalArgumentException("passwords do not match"))
                    .when(credentialValidationService)
                    .validateNewPassword(NEW_PASSWORD, DIFFERENT_CONFIRM_PASSWORD, CURRENT_PASSWORD_HASH);

            assertThrows(IllegalArgumentException.class, () -> passwordResetService.confirmPasswordReset(mismatchedDto, request));
            verifyNoInteractions(passwordResetVerificationService, passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenCodeVerificationFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new IllegalStateException("invalid code"))
                    .when(passwordResetVerificationService)
                    .verifyCodeOrThrow(EMAIL, settings, VERIFICATION_CODE);

            assertThrows(IllegalStateException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verify(passwordResetVerificationService, never()).getCurrentAttempts(EMAIL);
            verify(passwordResetVerificationService, never()).removeCode(settings);
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenCodeIsNull() {
            PasswordResetConfirmDto nullCodeDto = new PasswordResetConfirmDto(EMAIL, NEW_PASSWORD, CONFIRM_NEW_PASSWORD, null);
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            doThrow(new IllegalStateException("invalid code"))
                    .when(passwordResetVerificationService)
                    .verifyCodeOrThrow(eq(EMAIL), eq(settings), isNull());

            assertThrows(IllegalStateException.class, () -> passwordResetService.confirmPasswordReset(nullCodeDto, request));
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenGettingCurrentAttemptsFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenThrow(new IllegalStateException("attempts error"));

            assertThrows(IllegalStateException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verify(passwordResetVerificationService, never()).removeCode(settings);
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenCodeRemovalFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenReturn(attemptsDto);
            doThrow(new IllegalStateException("remove failed"))
                    .when(passwordResetVerificationService)
                    .removeCode(settings);

            assertThrows(IllegalStateException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenPasswordUpdateFails() {
            when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(user.getAccountSettings()).thenReturn(settings);
            when(user.getPassword()).thenReturn(CURRENT_PASSWORD_HASH);
            when(passwordResetVerificationService.getCurrentAttempts(EMAIL)).thenReturn(attemptsDto);
            doThrow(new IllegalStateException("update failed"))
                    .when(passwordUpdateService)
                    .updatePassword(user, NEW_PASSWORD, request);

            assertThrows(IllegalStateException.class, () -> passwordResetService.confirmPasswordReset(dto, request));
        }
    }
}