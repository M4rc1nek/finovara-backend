package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserManagerService userManagerService;
    private final PasswordManagementService passwordManagementService;

    public void changePassword(Long userId, PasswordRequestDto passwordRequestDto, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        String newPassword = passwordRequestDto.changePasswordDto().newPassword();

        if (!newPassword.equals(passwordRequestDto.changePasswordDto().confirmNewPassword())) {
            throw new MissingRequirementException("New passwords have to be the same");
        }

        if (newPassword.isEmpty()) {
            throw new MissingRequirementException("The new password cannot be empty");
        }

        passwordManagementService.updatePassword(user, newPassword, request);
    }
}
