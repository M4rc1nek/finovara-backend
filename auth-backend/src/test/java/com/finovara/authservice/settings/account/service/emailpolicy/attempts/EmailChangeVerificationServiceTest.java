package com.finovara.authservice.settings.account.service.emailpolicy.attempts;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.VerificationCodeAttemptsTemplate;
import com.finovara.authservice.util.attempts.VerificationCodeVerifier;
import com.finovara.authservice.util.attempts.properties.VerificationCodeProperties;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailChangeVerificationServiceTest {

    @Mock private SecretGenerator secretGenerator;
    @Mock private VerificationCodeVerifier verificationCodeVerifier;
    @Mock private VerificationCodeAttemptsTemplate attemptsTemplate;
    @Mock private AccountRepository accountRepository;
    @Mock private VerificationCodeProperties properties;
    @Mock private AccountSettings settings;
    @InjectMocks private EmailChangeVerificationService service;

    @Nested
    class GenerateCode {
        @Test
        void shouldGeneratePersistAndReturnCodeWhenPendingEmailIsProvided() {
            when(secretGenerator.generateSecureCode()).thenReturn(123456);
            when(properties.getCodeExpirationMinutes()).thenReturn(15);

            int result = service.generateCode(settings, "new@example.com");

            assertEquals(123456, result);
            verify(settings).setEmailChangeCode(123456);
            verify(settings).setPendingEmail("new@example.com");
            verify(accountRepository).save(settings);
        }
    }

    @Nested
    class VerifyCodeOrThrow {
        @BeforeEach
        void setUp() {
            when(properties.getMaxAttempts()).thenReturn(3);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(10);
        }

        @Test
        void shouldDelegateVerificationWhenCodeIsProvided() {
            service.verifyCodeOrThrow(1L, settings, 123456);

            verify(verificationCodeVerifier).verifyAttemptsOrThrow(any(), any(), eq(123456), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenVerifierRejectsCode() {
            doThrow(new IllegalArgumentException()).when(verificationCodeVerifier)
                    .verifyAttemptsOrThrow(any(), any(), any(), any(), any());

            assertThrows(IllegalArgumentException.class, () -> service.verifyCodeOrThrow(1L, settings, 123456));
        }
    }

    @Nested
    class CurrentAttemptsAndRemoval {
        @Test
        void shouldReturnCurrentAttemptsWhenRequested() {
            AttemptsDto attempts = new AttemptsDto(1, 2, 3);
            when(properties.getMaxAttempts()).thenReturn(3);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(10);
            when(accountRepository.getEmailChangeAttemptsByUserId(1L)).thenReturn(1);
            when(attemptsTemplate.getCurrent(any(), eq(1))).thenReturn(attempts);

            assertEquals(attempts, service.getCurrentAttempts(1L));
        }

        @Test
        void shouldClearEmailChangeStateWhenRemovingCode() {
            service.removeCode(settings);

            verify(settings).setEmailChangeCode(null);
            verify(settings).setEmailChangeCodeExpiresAt(null);
            verify(settings).setPendingEmail(null);
            verify(settings).setEmailChangeAttempts(0);
            verify(accountRepository).save(settings);
        }
    }
}
