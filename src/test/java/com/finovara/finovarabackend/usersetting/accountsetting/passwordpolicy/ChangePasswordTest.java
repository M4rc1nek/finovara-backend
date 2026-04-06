package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy;

import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ChangePasswordService;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.PasswordManagementService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PasswordManagementService passwordManagementService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ChangePasswordService changePasswordService;

    private final String USER_EMAIL = "test@test.com";

    @Test
    void shouldChangePasswordSuccessfully() {
        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("newPass", "newPass");
        PasswordRequestDto passwordRequestDto =
                new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        User user = new User();
        user.setEmail(USER_EMAIL);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);

        changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request);

        verify(passwordManagementService).updatePassword(user, "newPass", request);
    }

    @Test
    void shouldThrowExceptionWhenPasswordsDoNotMatch() {
        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("newPass1", "newPass2");
        PasswordRequestDto passwordRequestDto =
                new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        User user = new User();
        user.setEmail(USER_EMAIL);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);

        assertThrows(MissingRequirementException.class, () -> changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request));

        verify(passwordManagementService, never()).updatePassword(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenNewPasswordIsEmpty() {
        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("oldPass");
        ChangePasswordDto changePasswordDto = new ChangePasswordDto("", "");
        PasswordRequestDto passwordRequestDto =
                new PasswordRequestDto(confirmPasswordDto, changePasswordDto, null);

        User user = new User();
        user.setEmail(USER_EMAIL);

        when(userManagerService.getUserByEmailOrThrow(USER_EMAIL)).thenReturn(user);

        assertThrows(MissingRequirementException.class, () -> changePasswordService.changePassword(USER_EMAIL, passwordRequestDto, request));

        verify(passwordManagementService, never()).updatePassword(any(), any(), any());
    }
}