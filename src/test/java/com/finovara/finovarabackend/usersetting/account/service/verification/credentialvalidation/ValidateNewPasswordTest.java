package com.finovara.finovarabackend.usersetting.account.service.verification.credentialvalidation;

import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
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
public class ValidateNewPasswordTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CredentialValidationService credentialValidationService;

    private final String currentPasswordHash = "hashedPassword";

    static Stream<Arguments> invalidPasswordsProvider() {
        return Stream.of(
                Arguments.of(null, "confirm", "Password cannot be empty"),
                Arguments.of("   ", "confirm", "Password cannot be empty"),
                Arguments.of("password123", "different", "New passwords have to be the same"),
                Arguments.of("short", "short", "Password too short")
        );
    }

    @Test
    void shouldPassWhenPasswordIsValid() {
        when(passwordEncoder.matches("newPassword123", currentPasswordHash)).thenReturn(false);

        assertDoesNotThrow(() -> credentialValidationService.validateNewPassword
                ("newPassword123", "newPassword123", currentPasswordHash));
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordsProvider")
    void shouldThrowForInvalidPasswords(String password, String confirm, String expectedMessage) {
        MissingRequirementException exception = assertThrows(MissingRequirementException.class,
                () -> credentialValidationService.validateNewPassword(password, confirm, currentPasswordHash));

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

