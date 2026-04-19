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
    private PasswordValidator passwordValidator;

    @Test
    void shouldNotThrowExceptionWhenPasswordMatches() {
        Long userId = 1L;
        ConfirmPasswordDto dto = new ConfirmPasswordDto("correctPassword");

        User user = new User();
        user.setPassword("encodedPassword");

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

        assertDoesNotThrow(() -> passwordValidator.validatePassword(userId, dto));
    }

    @Test
    void shouldThrowWrongPasswordExceptionWhenPasswordDoesNotMatch() {
        Long userId = 1L;
        ConfirmPasswordDto dto = new ConfirmPasswordDto("wrongPassword");

        User user = new User();
        user.setPassword("encodedPassword");

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(WrongPasswordException.class, () -> passwordValidator.validatePassword(userId, dto));
    }
}