package com.finovara.authservice.util.attempts;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.attempts.dto.AttemptsRegistrationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeVerifierTest {

    @Mock
    private VerificationCodeAttemptsTemplate attemptsTemplate;

    @Mock
    private AttemptsHandler attemptsHandler;

    private VerificationCodeVerifier verificationCodeVerifier;

    private AttemptsContext attemptsContext;

    @BeforeEach
    void setUp() {
        verificationCodeVerifier = new VerificationCodeVerifier(attemptsTemplate);
        attemptsContext = new AttemptsContext(3, 10, "Attempts exceeded");
    }

    @Nested
    class VerifyAttemptsOrThrowValidation {

        @Test
        void shouldRegisterFailedAttemptWhenProvidedCodeIsNull() {
            assertInvalidCodeAttempt(123456, LocalDateTime.now().plusMinutes(5), null);
        }

        @Test
        void shouldRegisterFailedAttemptWhenStoredCodeIsNull() {
            assertInvalidCodeAttempt(null, LocalDateTime.now().plusMinutes(5), 123456);
        }

        @Test
        void shouldRegisterFailedAttemptWhenExpiresAtIsNull() {
            assertInvalidCodeAttempt(123456, null, 123456);
        }

        @Test
        void shouldRegisterFailedAttemptWhenCodeIsExpired() {
            assertInvalidCodeAttempt(123456, LocalDateTime.now().minusMinutes(1), 123456);
        }

        @Test
        void shouldRegisterFailedAttemptWhenCodesDoNotMatch() {
            assertInvalidCodeAttempt(123456, LocalDateTime.now().plusMinutes(5), 654321);
        }

        private void assertInvalidCodeAttempt(Integer storedCode, LocalDateTime expiresAt, Integer providedCode) {
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true))
                    .thenReturn(new AttemptsRegistrationResult(new AttemptsDto(1, 3, 2), false));

            assertThrows(InvalidVerificationCodeException.class, () ->
                    verificationCodeVerifier.verifyAttemptsOrThrow(storedCode, expiresAt, providedCode, attemptsContext, attemptsHandler));

            verify(attemptsTemplate).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
        }
    }

    @Nested
    class VerifyAttemptsOrThrow {

        @Test
        void shouldNotRegisterAttemptWhenCodeIsCorrect() {
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, false))
                    .thenReturn(new AttemptsRegistrationResult(new AttemptsDto(0, 3, 3), false));

            verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 123456, attemptsContext, attemptsHandler);

            verify(attemptsTemplate).checkAndRegisterAttempt(attemptsContext, attemptsHandler, false);
        }

        @Test
        void shouldThrowVerificationAttemptsExceededExceptionWhenLimitExceededDuringRegistration() {
            AttemptsDto attemptsDto = new AttemptsDto(3, 3, 0);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, true);
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true)).thenReturn(registrationResult);

            assertThrows(VerificationAttemptsExceededException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
        }

        @Test
        void shouldThrowInvalidVerificationCodeExceptionWhenLimitNotExceeded() {
            AttemptsDto attemptsDto = new AttemptsDto(1, 3, 2);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
        }

        @Test
        void shouldRegisterAttemptWhenProvidedCodeIsNull() {
            AttemptsDto attemptsDto = new AttemptsDto(1, 3, 2);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), null, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
        }

        @Test
        void shouldRegisterAttemptWhenCodeExpired() {
            AttemptsDto attemptsDto = new AttemptsDto(1, 3, 2);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().minusMinutes(1), 123456, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
        }

        @Test
        void shouldThrowVerificationAttemptsExceededExceptionWhenWindowActiveAndAlreadyAtLimit() {
            AttemptsDto attemptsDto = new AttemptsDto(3, 3, 0);
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true))
                    .thenReturn(new AttemptsRegistrationResult(attemptsDto, true));

            assertThrows(VerificationAttemptsExceededException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
            verify(attemptsTemplate, never()).checkAndRegisterAttempt(attemptsContext, attemptsHandler, false);
        }

        @Test
        void shouldNotThrowWhenWindowExpiredEvenIfPreviousAttemptsReachedLimit() {
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, false))
                    .thenReturn(new AttemptsRegistrationResult(new AttemptsDto(3, 3, 0), false));

            assertDoesNotThrow(() -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 123456, attemptsContext, attemptsHandler));

            verify(attemptsTemplate).checkAndRegisterAttempt(attemptsContext, attemptsHandler, false);
        }

        @Test
        void shouldRegisterAttemptWhenWindowActiveButBelowLimit() {
            AttemptsDto attemptsDto = new AttemptsDto(2, 3, 1);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.checkAndRegisterAttempt(attemptsContext, attemptsHandler, true)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).checkAndRegisterAttempt(attemptsContext, attemptsHandler, true);
        }
    }
}