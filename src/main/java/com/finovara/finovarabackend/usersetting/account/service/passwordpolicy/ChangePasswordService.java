package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.service.AccountChangesActivityService;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordRequestDto;
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
    private final AccountChangesActivityService accountChangesActivityService;

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
        accountChangesActivityService.createAccountChangesActivity(email, AccountChangesActivityType.PASSWORD_CHANGED,request);
        passwordChangeEmailService.sendEmail(user);

    }
}
