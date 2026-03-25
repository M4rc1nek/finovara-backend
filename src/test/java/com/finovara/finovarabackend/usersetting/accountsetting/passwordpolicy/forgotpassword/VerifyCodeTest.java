package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy.forgotpassword;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ForgotPasswordService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyCodeTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private final String EMAIL = "test@test.com";

    private User user;
    private AccountSettings accountSettings;

    @BeforeEach
    void setup() {
        user = new User();
        accountSettings = new AccountSettings();
        user.setAccountSettings(accountSettings);
    }

    @Test
    void shouldPassWhenCodeMatches() {
        accountSettings.setForgotPasswordCode(123456);
        accountSettings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(2));
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);


        forgotPasswordService.verifyCode(EMAIL, new ForgotPasswordDto(EMAIL, 123456));
    }

    @Test
    void shouldThrowExceptionWhenCodeExpired(){
        accountSettings.setForgotPasswordCode(123456);
        accountSettings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        assertThrows(InvalidInputException.class, () -> forgotPasswordService.verifyCode(EMAIL, new ForgotPasswordDto(EMAIL, 123456)));

    }

    @Test
    void shouldThrowExceptionWhenCodeDoesNotMatch() {
        accountSettings.setForgotPasswordCode(123456);
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        assertThrows(InvalidInputException.class, () -> forgotPasswordService.verifyCode(EMAIL, new ForgotPasswordDto(EMAIL, 999999)));
    }

    @Test
    void shouldThrowExceptionWhenCodeIsNotGenerated(){
        accountSettings.setForgotPasswordCode(null);
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        assertThrows(InvalidInputException.class, () -> forgotPasswordService.verifyCode(EMAIL, new ForgotPasswordDto(EMAIL, null)));

    }
}