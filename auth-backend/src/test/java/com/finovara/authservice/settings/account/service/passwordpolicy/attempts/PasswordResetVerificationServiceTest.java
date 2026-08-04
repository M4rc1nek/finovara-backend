package com.finovara.authservice.settings.account.service.passwordpolicy.attempts;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.properties.VerificationCodeProperties;
import com.finovara.authservice.util.attempts.VerificationCodeAttemptsTemplate;
import com.finovara.authservice.util.attempts.VerificationCodeVerifier;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetVerificationServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final int GENERATED_CODE = 111222;

    @Mock
    private SecretGenerator secretGenerator;

    @Mock
    private VerificationCodeVerifier verificationCodeVerifier;

    @Mock
    private VerificationCodeAttemptsTemplate attemptsTemplate;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private VerificationCodeProperties properties;

    private PasswordResetVerificationService service;

    private AccountSettings settings;

    @BeforeEach
    void setUp() {
        service = new PasswordResetVerificationService(secretGenerator, verificationCodeVerifier, attemptsTemplate, accountRepository, properties);
        settings = mock(AccountSettings.class);
    }

    @Nested
    class GenerateCode {

        @Test
        void shouldGenerateAndPersistCodeWhenCalled() {
            when(secretGenerator.generateSecureCode()).thenReturn(GENERATED_CODE);
            when(properties.getCodeExpirationMinutes()).thenReturn(15);

            int result = service.generateCode(settings);

            assertEquals(GENERATED_CODE, result);
            verify(settings).setResetPasswordCode(GENERATED_CODE);
            verify(accountRepository).save(settings);
        }
    }

    @Nested
    class VerifyCodeOrThrow {

        @Test
        void shouldDelegateToVerifierWhenCalled() {
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            when(settings.getResetPasswordCode()).thenReturn(GENERATED_CODE);

            service.verifyCodeOrThrow(EMAIL, settings, GENERATED_CODE);

            verify(verificationCodeVerifier).verifyAttemptsOrThrow(eq(GENERATED_CODE), any(), eq(GENERATED_CODE), any(), any());
        }

        @Test
        void shouldPropagateExceptionWhenVerifierThrows() {
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            when(settings.getResetPasswordCode()).thenReturn(GENERATED_CODE);
            doThrow(new RuntimeException("invalid code"))
                    .when(verificationCodeVerifier)
                    .verifyAttemptsOrThrow(any(), any(), any(), any(), any());

            assertThrows(RuntimeException.class, () -> service.verifyCodeOrThrow(EMAIL, settings, 999999));
        }
    }

    @Nested
    class GetCurrentAttempts {

        @Test
        void shouldReturnCurrentAttemptsWhenCalled() {
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            when(accountRepository.getPasswordResetAttemptsByUserEmail(EMAIL)).thenReturn(3);
            when(attemptsTemplate.getCurrent(any(), eq(3))).thenReturn(new AttemptsDto(3, 5, 2));

            AttemptsDto result = service.getCurrentAttempts(EMAIL);

            assertEquals(3, result.used());
            assertEquals(2, result.remaining());
        }
    }

    @Nested
    class RemoveCode {

        @Test
        void shouldClearAllPasswordResetFieldsWhenCalled() {
            service.removeCode(settings);

            verify(settings).setResetPasswordCode(null);
            verify(settings).setResetPasswordCodeExpiresAt(null);
            verify(settings).setPasswordResetAttempts(0);
            verify(accountRepository, times(1)).save(settings);
        }
    }
}