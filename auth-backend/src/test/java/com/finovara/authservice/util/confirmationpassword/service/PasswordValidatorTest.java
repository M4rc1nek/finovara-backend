package com.finovara.authservice.util.confirmationpassword.service;

import com.finovara.authservice.user.model.User;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.exception.forbidden.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordValidator passwordValidator;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPassword("encodedPassword");
    }

    @Nested
    class ValidatePassword {

        @Test
        void shouldNotThrowExceptionWhenPasswordMatches() {
            Long userId = 1L;
            ConfirmPasswordDto dto = new ConfirmPasswordDto("correctPassword");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

            assertDoesNotThrow(() -> passwordValidator.validatePassword(userId, dto));
        }

        @Test
        void shouldThrowInvalidPasswordExceptionWhenPasswordDoesNotMatch() {
            Long userId = 1L;
            ConfirmPasswordDto dto = new ConfirmPasswordDto("wrongPassword");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            assertThrows(InvalidPasswordException.class, () -> passwordValidator.validatePassword(userId, dto));
        }

        @Test
        void shouldThrowInvalidPasswordExceptionWhenPasswordIsNull() {
            Long userId = 1L;
            ConfirmPasswordDto dto = new ConfirmPasswordDto(null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            when(passwordEncoder.matches(null, "encodedPassword")).thenReturn(false);

            assertThrows(InvalidPasswordException.class, () -> passwordValidator.validatePassword(userId, dto));
        }

        @Test
        void shouldThrowInvalidPasswordExceptionWhenPasswordIsEmpty() {
            Long userId = 1L;
            ConfirmPasswordDto dto = new ConfirmPasswordDto("");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(testUser);
            when(passwordEncoder.matches("", "encodedPassword")).thenReturn(false);

            assertThrows(InvalidPasswordException.class, () -> passwordValidator.validatePassword(userId, dto));
        }
    }
}