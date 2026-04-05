package com.finovara.finovarabackend.util.confirmationpassword.service;

import com.finovara.finovarabackend.exception.unauthorized.WrongPasswordException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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
class PasswordConfirmationTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordConfirmationService passwordConfirmationService;

    @Test
    void shouldNotThrowExceptionWhenPasswordMatches() {
        String email = "test@example.com";
        ConfirmPasswordDto dto = new ConfirmPasswordDto("correctPassword");

        User user = new User();
        user.setPassword("encodedPassword");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

        assertDoesNotThrow(() -> passwordConfirmationService.confirmPassword(email, dto));
    }

    @Test
    void shouldThrowWrongPasswordExceptionWhenPasswordDoesNotMatch() {
        String email = "test@example.com";
        ConfirmPasswordDto dto = new ConfirmPasswordDto("wrongPassword");

        User user = new User();
        user.setPassword("encodedPassword");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(WrongPasswordException.class, () -> passwordConfirmationService.confirmPassword(email, dto));
    }
}