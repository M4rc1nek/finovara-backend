package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.change;

import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private CredentialValidationService credentialValidationService;

    @Mock
    private PasswordUpdateService passwordUpdateService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ChangePasswordService changePasswordService;

    ChangePasswordDto dto;
    Long userId;
    String newPassword;

    @BeforeEach
    void setUp() {
        userId = 1L;
        newPassword = "newPass";
        dto = new ChangePasswordDto(newPassword, newPassword);
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        User user = new User();
        String oldPassword = "oldPass";

        user.setPassword(oldPassword);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        changePasswordService.changePassword(userId, dto, request);

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(credentialValidationService).validateNewPassword(newPassword, newPassword, oldPassword);
        verify(passwordUpdateService).updatePassword(user, newPassword, request);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> changePasswordService.changePassword(userId, dto, request));

        verifyNoInteractions(credentialValidationService);
        verifyNoInteractions(passwordUpdateService);
    }

}