package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ChangePasswordService;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PasswordConfirmationService passwordConfirmationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordChangeEmailService passwordChangeEmailService;
    @Mock
    private AccountChangesActivityService accountChangesActivityService;
    @Mock
    private HttpServletRequest request;
    @InjectMocks
    private ChangePasswordService changePasswordService;

    private final String USER_EMAIL = "test@test.com";

    @Test
    void shouldChangePasswordAndSendNotification() {

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("newPass", "newPass");
        PasswordRequestDto passwordRequestDto = new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        NotificationSettings settings = new NotificationSettings();
        settings.setNotifyOnPasswordChange(true);

        User user = new User();
        user.setEmail(USER_EMAIL);
        user.setNotificationSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedPass");

        changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request);

        verify(passwordConfirmationService).confirmPassword(USER_EMAIL, confirmPasswordDto);
        verify(userRepository).save(user);
        verify(accountChangesActivityService).createAccountChangesActivity(USER_EMAIL, AccountChangesActivityType.PASSWORD_CHANGED, request);
        verify(passwordChangeEmailService).sendEmail(user);
    }

    @Test
    void shouldChangePasswordWithoutNotification() {

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("newPass", "newPass");
        PasswordRequestDto passwordRequestDto = new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        NotificationSettings settings = new NotificationSettings();
        settings.setNotifyOnPasswordChange(false);

        User user = new User();
        user.setEmail(USER_EMAIL);
        user.setNotificationSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedPass");

        changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request);

        verify(passwordChangeEmailService, never()).sendEmail(user);
        verify(userRepository).save(user);
        verify(accountChangesActivityService).createAccountChangesActivity(USER_EMAIL, AccountChangesActivityType.PASSWORD_CHANGED, request);
    }

    @Test
    void shouldThrowExceptionWhenPasswordsDoNotMatch() {

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("newPass1", "newPass2");
        PasswordRequestDto passwordRequestDto = new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        User user = new User();
        user.setEmail(USER_EMAIL);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);

        assertThrows(MissingRequirementException.class, () -> changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenNewPasswordIsEmpty() {

        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("", "");
        PasswordRequestDto passwordRequestDto = new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        User user = new User();
        user.setEmail(USER_EMAIL);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);

        assertThrows(MissingRequirementException.class, () -> changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request));

        verify(userRepository, never()).save(any());
    }
}