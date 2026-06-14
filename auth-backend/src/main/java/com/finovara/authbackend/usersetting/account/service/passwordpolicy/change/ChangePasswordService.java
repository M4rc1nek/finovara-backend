package com.finovara.authbackend.usersetting.account.service.passwordpolicy.change;

import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.usersetting.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.authbackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.authbackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserManagerService userManagerService;
    private final CredentialValidationService credentialValidationService;
    private final PasswordUpdateService passwordUpdateService;

    public void changePassword(Long userId, ChangePasswordDto changePasswordDto, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        String newPassword = changePasswordDto.newPassword();

        credentialValidationService.validateNewPassword(newPassword, changePasswordDto.confirmNewPassword(), user.getPassword());

        passwordUpdateService.updatePassword(user, newPassword, request);
    }
}