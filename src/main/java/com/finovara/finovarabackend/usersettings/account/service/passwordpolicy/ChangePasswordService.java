package com.finovara.finovarabackend.usersettings.account.service.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.accountchanges.model.UserActivityAccountChangesType;
import com.finovara.finovarabackend.accountactivity.accountchanges.service.UserActivityAccountChangesService;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final UserActivityAccountChangesService userActivityAccountChangesService;

    public void changePassword(String email, PasswordRequestDto passwordRequestDto, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        if (!passwordRequestDto.changePasswordDto().newPassword()
                .equals(passwordRequestDto.changePasswordDto().confirmNewPassword())) {
            throw new MissingRequirementException("New passwords have to be the same");
        }

        if (passwordRequestDto.changePasswordDto().newPassword().isEmpty()) {
            throw new MissingRequirementException("The new password cannot be empty");
        }

        passwordConfirmationService.confirmPassword(email, passwordRequestDto.confirmPasswordDto());

        user.setPassword(passwordEncoder.encode(passwordRequestDto.changePasswordDto().newPassword()));
        userRepository.save(user);
        userActivityAccountChangesService.createUserActivityAccountChanges(email, UserActivityAccountChangesType.PASSWORD_CHANGED,request);
        passwordChangeEmailService.sendEmail(user);

    }
}
