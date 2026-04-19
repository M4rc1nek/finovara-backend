package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy.forgotpassword;

import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.repository.AccountRepository;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ForgotPasswordService;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.PasswordUpdateService;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordWithCodeTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordUpdateService passwordUpdateService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private User user;
    private AccountSettings accountSettings;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        accountSettings = new AccountSettings();
        NotificationEmailSettings notificationEmailSettings = new NotificationEmailSettings();
        notificationEmailSettings.setNotifyOnPasswordChange(true);

        user.setAccountSettings(accountSettings);
        user.setNotificationEmailSettings(notificationEmailSettings);
        user.setPassword("encodedOldPassword");
    }

    private PasswordRequestDto buildPasswordRequest(String newPassword, String confirmPassword, Integer code) {
        accountSettings.setForgotPasswordCode(code);
        return new PasswordRequestDto(
                new ConfirmPasswordDto(confirmPassword),
                new ChangePasswordDto(newPassword, confirmPassword),
                new ForgotPasswordDto(EMAIL, code)
        );
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        accountSettings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("newPass", "encodedOldPassword")).thenReturn(false);

        PasswordRequestDto dto = buildPasswordRequest("newPass", "newPass", 123456);

        forgotPasswordService.changePasswordWithCode(EMAIL, dto, request);

        verify(accountRepository).save(accountSettings);
        verify(passwordUpdateService).updatePassword(user, "newPass", request);
    }

    @Test
    void shouldThrowExceptionWhenNewPasswordsDoNotMatch() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        accountSettings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        PasswordRequestDto dto = buildPasswordRequest("pass1", "pass2", 123456);

        assertThrows(MissingRequirementException.class, () -> forgotPasswordService.changePasswordWithCode(EMAIL, dto, request));
    }

    @Test
    void shouldThrowExceptionWhenNewPasswordAlreadySet() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        accountSettings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedOldPassword")).thenReturn(true);

        PasswordRequestDto dto = buildPasswordRequest("oldPass", "oldPass", 123456);

        assertThrows(MissingRequirementException.class,
                () -> forgotPasswordService.changePasswordWithCode(EMAIL, dto, request));
    }

    @Test
    void shouldThrowExceptionWhenNewPasswordIsEmpty() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        accountSettings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        PasswordRequestDto dto = buildPasswordRequest("", "", 123456);

        assertThrows(MissingRequirementException.class,
                () -> forgotPasswordService.changePasswordWithCode(EMAIL, dto, request));
    }
}