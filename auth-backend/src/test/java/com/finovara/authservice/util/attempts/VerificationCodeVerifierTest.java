package com.finovara.authservice.util.attempts;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.attempts.dto.AttemptsRegistrationResult;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
    class VerifyOrThrow {

        @Test
        void shouldNotThrowExceptionWhenCodeIsCorrect() {
            assertDoesNotThrow(() -> verificationCodeVerifier.verifyOrThrow(123456, LocalDateTime.now().plusMinutes(5), 123456));
        }

        @Test
        void shouldThrowExceptionWhenProvidedCodeIsNull() {
            assertThrows(InvalidInputException.class, () -> verificationCodeVerifier.verifyOrThrow(123456, LocalDateTime.now().plusMinutes(5), null));
        }

        @Test
        void shouldThrowExceptionWhenStoredCodeIsNull() {
            assertThrows(InvalidInputException.class, () -> verificationCodeVerifier.verifyOrThrow(null, LocalDateTime.now().plusMinutes(5), 123456));
        }

        @Test
        void shouldThrowExceptionWhenExpiresAtIsNull() {
            assertThrows(InvalidInputException.class, () -> verificationCodeVerifier.verifyOrThrow(123456, null, 123456));
        }

        @Test
        void shouldThrowExceptionWhenExpiresAtIsInThePast() {
            assertThrows(InvalidInputException.class, () -> verificationCodeVerifier.verifyOrThrow(123456, LocalDateTime.now().minusMinutes(1), 123456));
        }

        @Test
        void shouldThrowExceptionWhenCodesDoNotMatch() {
            assertThrows(InvalidInputException.class, () -> verificationCodeVerifier.verifyOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321));
        }
    }

    @Nested
    class VerifyAttemptsOrThrow {

        @Test
        void shouldNotRegisterAttemptWhenCodeIsCorrect() {
            verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 123456, attemptsContext, attemptsHandler);

            verifyNoInteractions(attemptsTemplate);
        }

        @Test
        void shouldThrowVerificationAttemptsExceededExceptionWhenLimitExceededDuringRegistration() {
            AttemptsDto attemptsDto = new AttemptsDto(3, 3, 0);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, true);
            when(attemptsTemplate.registerAttempt(attemptsContext, attemptsHandler)).thenReturn(registrationResult);

            assertThrows(VerificationAttemptsExceededException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).registerAttempt(attemptsContext, attemptsHandler);
        }

        @Test
        void shouldThrowInvalidVerificationCodeExceptionWhenLimitNotExceeded() {
            AttemptsDto attemptsDto = new AttemptsDto(1, 3, 2);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.registerAttempt(attemptsContext, attemptsHandler)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).registerAttempt(attemptsContext, attemptsHandler);
        }

        @Test
        void shouldRegisterAttemptWhenProvidedCodeIsNull() {
            AttemptsDto attemptsDto = new AttemptsDto(1, 3, 2);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.registerAttempt(attemptsContext, attemptsHandler)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), null, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).registerAttempt(attemptsContext, attemptsHandler);
        }

        @Test
        void shouldRegisterAttemptWhenCodeExpired() {
            AttemptsDto attemptsDto = new AttemptsDto(1, 3, 2);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.registerAttempt(attemptsContext, attemptsHandler)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().minusMinutes(1), 123456, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).registerAttempt(attemptsContext, attemptsHandler);
        }

        @Test
        void shouldThrowVerificationAttemptsExceededExceptionWhenWindowActiveAndAlreadyAtLimit() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.getCurrentAttempts()).thenReturn(3);
            AttemptsDto attemptsDto = new AttemptsDto(3, 3, 0);
            when(attemptsTemplate.getCurrent(attemptsContext, 3)).thenReturn(attemptsDto);

            assertThrows(VerificationAttemptsExceededException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).getCurrent(attemptsContext, 3);
            verify(attemptsTemplate, never()).registerAttempt(attemptsContext, attemptsHandler);
        }

        @Test
        void shouldNotThrowWhenWindowExpiredEvenIfPreviousAttemptsReachedLimit() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

            assertDoesNotThrow(() -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 123456, attemptsContext, attemptsHandler));

            verifyNoInteractions(attemptsTemplate);
        }

        @Test
        void shouldRegisterAttemptWhenWindowActiveButBelowLimit() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.getCurrentAttempts()).thenReturn(1);
            AttemptsDto attemptsDto = new AttemptsDto(2, 3, 1);
            AttemptsRegistrationResult registrationResult = new AttemptsRegistrationResult(attemptsDto, false);
            when(attemptsTemplate.registerAttempt(attemptsContext, attemptsHandler)).thenReturn(registrationResult);

            assertThrows(InvalidVerificationCodeException.class, () -> verificationCodeVerifier.verifyAttemptsOrThrow(123456, LocalDateTime.now().plusMinutes(5), 654321, attemptsContext, attemptsHandler));

            verify(attemptsTemplate, times(1)).registerAttempt(attemptsContext, attemptsHandler);
            verify(attemptsTemplate, never()).getCurrent(attemptsContext, 1);
        }
    }
}