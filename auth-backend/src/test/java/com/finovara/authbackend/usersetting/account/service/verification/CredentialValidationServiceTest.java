package com.finovara.authbackend.usersetting.account.service.verification;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CredentialValidationService credentialValidationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("old@mail.com");
    }

    @Nested
    class ValidateEmailChange {
        static Stream<Arguments> invalidEmailsProvider() {
            return Stream.of(Arguments.of(null, "Email cannot be empty"),
                    Arguments.of("", "Email cannot be empty"),
                    Arguments.of("   ", "Email cannot be empty"));
        }

        @ParameterizedTest
        @MethodSource("invalidEmailsProvider")
        void shouldThrowWhenEmailIsEmpty(String email, String expectedMessage) {
            MissingRequirementException exception = assertThrows(MissingRequirementException.class, () -> credentialValidationService.validateEmailChange(user, email));

            assertEquals(expectedMessage, exception.getMessage());
        }

        @Test
        void shouldThrowWhenEmailIsSameAsCurrent() {
            InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                    credentialValidationService.validateEmailChange(user, "OLD@mail.com"));

            assertEquals("New mail cannot be the same", exception.getMessage());
        }

        @Test
        void shouldThrowWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail("new@mail.com")).thenReturn(true);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () ->
                    credentialValidationService.validateEmailChange(user, "new@mail.com"));

            assertEquals("Email already in use", exception.getMessage());
        }

        @Test
        void shouldPassWhenEmailIsValid() {
            when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);

            assertDoesNotThrow(() -> credentialValidationService.validateEmailChange(user, "new@mail.com"));
        }
    }

    @Nested
    class ValidateNewPassword {
        private final String currentPasswordHash = "hashedPassword";

        static Stream<Arguments> invalidPasswordsProvider() {
            return Stream.of(Arguments.of(null, "confirm", "Password cannot be empty"), Arguments.of("   ", "confirm", "Password cannot be empty"), Arguments.of("password123", "different", "New passwords have to be the same"), Arguments.of("short", "short", "Password too short"));
        }

        @Test
        void shouldPassWhenPasswordIsValid() {
            when(passwordEncoder.matches("newPassword123", currentPasswordHash)).thenReturn(false);

            assertDoesNotThrow(() -> credentialValidationService.validateNewPassword("newPassword123", "newPassword123", currentPasswordHash));
        }

        @ParameterizedTest
        @MethodSource("invalidPasswordsProvider")
        void shouldThrowForInvalidPasswords(String password, String confirm, String expectedMessage) {
            MissingRequirementException exception = assertThrows(MissingRequirementException.class, () ->
                    credentialValidationService.validateNewPassword(password, confirm, currentPasswordHash));

            assertEquals(expectedMessage, exception.getMessage());
        }

        @Test
        void shouldThrowWhenPasswordIsSameAsCurrent() {
            when(passwordEncoder.matches("password123", currentPasswordHash)).thenReturn(true);

            MissingRequirementException exception = assertThrows(MissingRequirementException.class, () ->
                    credentialValidationService.validateNewPassword("password123", "password123", currentPasswordHash));

            assertEquals("This password is already set", exception.getMessage());

            verify(passwordEncoder).matches("password123", currentPasswordHash);
        }
    }
}