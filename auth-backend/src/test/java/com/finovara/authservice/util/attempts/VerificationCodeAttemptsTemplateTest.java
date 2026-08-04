package com.finovara.authservice.util.attempts;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeAttemptsTemplateTest {

    @Mock
    private AttemptsHandler attemptsHandler;

    private VerificationCodeAttemptsTemplate verificationCodeAttemptsTemplate;

    private AttemptsContext attemptsContext;

    @BeforeEach
    void setUp() {
        verificationCodeAttemptsTemplate = new VerificationCodeAttemptsTemplate();
        attemptsContext = new AttemptsContext(3, 10, "Attempts exceeded");
    }

    @Nested
    class RegisterAttempt {

        @Test
        void shouldResetAttemptsWhenExpiresAtIsNull() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(null);
            when(attemptsHandler.incrementAttempts(3)).thenReturn(1);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(1);

            AttemptsRegistrationResult result = verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            verify(attemptsHandler, times(1)).resetAttempts(eq(0), any(LocalDateTime.class));
            assertFalse(result.limitExceeded());
        }

        @Test
        void shouldResetAttemptsWhenExpiresAtIsInThePast() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(5));
            when(attemptsHandler.incrementAttempts(3)).thenReturn(1);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(1);

            verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            verify(attemptsHandler, times(1)).resetAttempts(eq(0), any(LocalDateTime.class));
        }

        @Test
        void shouldNotResetAttemptsWhenExpiresAtIsInTheFuture() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.incrementAttempts(3)).thenReturn(1);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(1);

            verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            verify(attemptsHandler, never()).resetAttempts(anyInt(), any(LocalDateTime.class));
        }

        @Test
        void shouldReturnLimitNotExceededWhenIncrementSucceeds() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.incrementAttempts(3)).thenReturn(1);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(2);

            AttemptsRegistrationResult result = verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            assertFalse(result.limitExceeded());
        }

        @Test
        void shouldReturnLimitExceededWhenIncrementFails() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.incrementAttempts(3)).thenReturn(0);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(3);

            AttemptsRegistrationResult result = verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            assertTrue(result.limitExceeded());
        }

        @Test
        void shouldReturnZeroRemainingWhenAttemptsEqualMaxAttempts() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.incrementAttempts(3)).thenReturn(0);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(3);

            AttemptsRegistrationResult result = verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            assertEquals(0, result.attempts().remaining());
        }

        @Test
        void shouldReturnPositiveRemainingWhenAttemptsBelowMaxAttempts() {
            when(attemptsHandler.getAttemptsExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(attemptsHandler.incrementAttempts(3)).thenReturn(1);
            when(attemptsHandler.getCurrentAttempts()).thenReturn(1);

            AttemptsRegistrationResult result = verificationCodeAttemptsTemplate.registerAttempt(attemptsContext, attemptsHandler);

            assertEquals(2, result.attempts().remaining());
        }
    }

    @Nested
    class GetCurrent {

        @Test
        void shouldReturnAttemptsDtoWithRemainingWhenCurrentAttemptsBelowMax() {
            AttemptsDto result = verificationCodeAttemptsTemplate.getCurrent(attemptsContext, 1);

            assertEquals(1, result.used());
            assertEquals(3, result.max());
            assertEquals(2, result.remaining());
        }

        @Test
        void shouldReturnZeroRemainingWhenCurrentAttemptsEqualsMax() {
            AttemptsDto result = verificationCodeAttemptsTemplate.getCurrent(attemptsContext, 3);

            assertEquals(0, result.remaining());
        }

        @Test
        void shouldReturnZeroRemainingWhenCurrentAttemptsExceedMax() {
            AttemptsDto result = verificationCodeAttemptsTemplate.getCurrent(attemptsContext, 5);

            assertEquals(0, result.remaining());
        }
    }
}