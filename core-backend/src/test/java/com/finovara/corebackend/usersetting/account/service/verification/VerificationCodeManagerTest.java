package com.finovara.corebackend.usersetting.account.service.verification;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.corebackend.usersetting.account.dto.AttemptsDto;
import com.finovara.corebackend.usersetting.account.model.AccountSettings;
import com.finovara.corebackend.usersetting.account.repository.AccountRepository;
import com.finovara.corebackend.usersetting.account.service.verification.properties.VerificationCodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeManagerTest {

    @Mock
    private VerificationCodeProperties properties;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private VerificationCodeManager verificationCodeManager;

    private AccountSettings settings;

    private final Long userId = 1L;
    private final String email = "test@mail.com";

    @BeforeEach
    void setUp() {
        settings = new AccountSettings();
    }

    @Nested
    class EmailChangeCode {
        @Test
        void shouldGenerateEmailChangeCode() {
            when(properties.getCodeExpirationMinutes()).thenReturn(15);

            int code = verificationCodeManager.generateEmailChangeCode(settings, "new@mail.com");

            assertNotNull(settings.getEmailChangeCode());
            assertEquals("new@mail.com", settings.getPendingEmail());
            assertTrue(code >= 100000 && code <= 999999);

            verify(accountRepository).save(settings);
        }

        @Test
        void shouldVerifyEmailChangeCodeSuccessfully() {
            settings.setEmailChangeCode(123456);
            settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

            assertDoesNotThrow(() -> verificationCodeManager.verifyEmailChangeCode(settings, 123456));
        }

        @Test
        void shouldThrowWhenEmailCodeIsWrong() {
            settings.setEmailChangeCode(111111);
            settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

            assertThrows(InvalidInputException.class, () -> verificationCodeManager.verifyEmailChangeCode(settings, 999999));
        }

        @Test
        void shouldThrowWhenEmailCodeExpired() {
            settings.setEmailChangeCode(111111);
            settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

            assertThrows(InvalidInputException.class, () -> verificationCodeManager.verifyEmailChangeCode(settings, 111111));
        }

        @Test
        void shouldRemoveEmailChangeCode() {
            settings.setEmailChangeCode(123456);
            settings.setPendingEmail("test@mail.com");
            settings.setEmailChangeAttempts(3);

            verificationCodeManager.removeEmailChangeCode(settings);

            assertNull(settings.getEmailChangeCode());
            assertNull(settings.getEmailChangeCodeExpiresAt());
            assertNull(settings.getPendingEmail());
            assertEquals(0, settings.getEmailChangeAttempts());

            verify(accountRepository).save(settings);
        }
    }

    @Nested
    class PasswordResetCode {
        @Test
        void shouldGeneratePasswordResetCode() {
            when(properties.getCodeExpirationMinutes()).thenReturn(15);

            int code = verificationCodeManager.generatePasswordResetCode(settings);

            assertNotNull(settings.getResetPasswordCode());
            assertTrue(code >= 100000 && code <= 999999);

            verify(accountRepository).save(settings);
        }

        @Test
        void shouldVerifyPasswordResetCode() {
            settings.setResetPasswordCode(123456);
            settings.setResetPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

            assertDoesNotThrow(() -> verificationCodeManager.verifyPasswordResetCode(settings, 123456));
        }

        @Test
        void shouldThrowWhenPasswordResetCodeWrong() {
            settings.setResetPasswordCode(123456);
            settings.setResetPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

            assertThrows(InvalidInputException.class, () -> verificationCodeManager.verifyPasswordResetCode(settings, 999999));
        }

        @Test
        void shouldThrowWhenPasswordResetCodeExpired() {
            settings.setResetPasswordCode(123456);
            settings.setResetPasswordCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

            assertThrows(InvalidInputException.class, () -> verificationCodeManager.verifyPasswordResetCode(settings, 123456));
        }

        @Test
        void shouldRemovePasswordResetCode() {
            settings.setResetPasswordCode(123456);
            settings.setPasswordResetAttempts(2);

            verificationCodeManager.removePasswordResetCode(settings);

            assertNull(settings.getResetPasswordCode());
            assertNull(settings.getResetPasswordCodeExpiresAt());
            assertEquals(0, settings.getPasswordResetAttempts());

            verify(accountRepository).save(settings);
        }
    }

    @Nested
    class EmailChangeAttempts {

        @Test
        void shouldResetAttemptsWhenExpired() {
            settings.setAttemptsEmailExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(10);

            when(accountRepository.incrementEmailChangeAttempts(userId, 5)).thenReturn(1);
            when(accountRepository.getEmailChangeAttemptsByUserId(userId)).thenReturn(2);

            AttemptsDto result = verificationCodeManager.verifyEmailChangeAttemptsCode(userId, settings);

            assertEquals(2, result.used());
            assertEquals(5, result.max());
            assertEquals(3, result.remaining());

            verify(accountRepository).save(settings);
        }

        @Test
        void shouldThrowWhenLimitExceeded() {
            settings.setAttemptsEmailExpiresAt(LocalDateTime.now().plusMinutes(10));

            when(properties.getMaxAttempts()).thenReturn(5);

            when(accountRepository.incrementEmailChangeAttempts(userId, 5)).thenReturn(0);
            when(accountRepository.getEmailChangeAttemptsByUserId(userId)).thenReturn(5);

            AttemptsDto result = new AttemptsDto(5, 5, 0);

            VerificationAttemptsExceededException ex = assertThrows(VerificationAttemptsExceededException.class, () -> verificationCodeManager.verifyEmailChangeAttemptsCode(userId, settings));

            assertEquals(result.used(), ex.getAttempts().used());
        }

        @Test
        void shouldReturnCurrentAttempts() {
            when(properties.getMaxAttempts()).thenReturn(5);
            when(accountRepository.getEmailChangeAttemptsByUserId(userId)).thenReturn(2);

            AttemptsDto result = verificationCodeManager.getCurrentEmailChangeAttempts(userId);

            assertEquals(2, result.used());
            assertEquals(5, result.max());
            assertEquals(3, result.remaining());
        }

    }

    @Nested
    class PasswordResetAttempts {

        @Test
        void shouldResetPasswordAttemptsWhenExpired() {
            settings.setAttemptsPasswordExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(10);

            when(accountRepository.incrementPasswordResetAttempts(email, 5)).thenReturn(1);
            when(accountRepository.getPasswordResetAttemptsByUserEmail(email)).thenReturn(1);

            AttemptsDto result = verificationCodeManager.verifyPasswordResetAttemptsCode(email, settings);

            assertEquals(1, result.used());
            assertEquals(5, result.max());
            assertEquals(4, result.remaining());

            verify(accountRepository).save(settings);
        }

        @Test
        void shouldThrowWhenPasswordLimitExceeded() {
            settings.setAttemptsPasswordExpiresAt(LocalDateTime.now().plusMinutes(10));

            when(properties.getMaxAttempts()).thenReturn(5);

            when(accountRepository.incrementPasswordResetAttempts(email, 5)).thenReturn(0);
            when(accountRepository.getPasswordResetAttemptsByUserEmail(email)).thenReturn(5);

            assertThrows(VerificationAttemptsExceededException.class, () -> verificationCodeManager.verifyPasswordResetAttemptsCode(email, settings));
        }

        @Test
        void shouldReturnCurrentPasswordAttempts() {
            when(properties.getMaxAttempts()).thenReturn(5);
            when(accountRepository.getPasswordResetAttemptsByUserEmail(email)).thenReturn(3);

            AttemptsDto result = verificationCodeManager.getCurrentPasswordResetAttempts(email);

            assertEquals(3, result.used());
            assertEquals(5, result.max());
            assertEquals(2, result.remaining());
        }

    }
}