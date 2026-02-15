package com.finovara.finovarabackend.usersettings.account.service;

import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.account.dto.ChangePasswordRequestDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserManagerService userManagerService;
    private final PasswordConfirmationService passwordConfirmationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordChangeEmailService passwordChangeEmailService;

    public void changePassword(String email, ChangePasswordRequestDto changePasswordRequestDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        if (!changePasswordRequestDto.changePasswordDto().newPassword()
                .equals(changePasswordRequestDto.changePasswordDto().confirmNewPassword())) {
            throw new MissingRequirementException("New passwords have to be the same");
        }

        if (changePasswordRequestDto.changePasswordDto().newPassword().isEmpty()) {
            throw new MissingRequirementException("The new password cannot be empty");
        }

        passwordConfirmationService.confirmPassword(email, changePasswordRequestDto.confirmPasswordDto());

        user.setPassword(passwordEncoder.encode(changePasswordRequestDto.changePasswordDto().newPassword()));
        userRepository.save(user);
        passwordChangeEmailService.sendEmail(user);

    }
}
