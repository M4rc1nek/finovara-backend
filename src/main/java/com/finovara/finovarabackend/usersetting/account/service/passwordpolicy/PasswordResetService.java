package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.finovarabackend.usersetting.account.service.security.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.security.VerificationCodeManager;
import com.finovara.finovarabackend.usersetting.account.service.security.VerificationCodeEmailService;
import com.finovara.finovarabackend.usersetting.notificationemail.action.passwordchange.service.NotifyPasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final CredentialValidationService credentialValidationService;
    private final VerificationCodeManager verificationCodeManager;
    private final VerificationCodeEmailService verificationCodeEmailService;
    private final NotifyPasswordChangeService notifyPasswordChangeService;
    private final PasswordUpdateService passwordUpdateService;

    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto dto) {
        User user = getUserByEmailOrThrow(dto.email());

        generateAndSendPasswordResetCode(user, dto.email());
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDto dto, HttpServletRequest request) {
        User user = getUserByEmailOrThrow(dto.email());
        AccountSettings accountSettings = user.getAccountSettings();

        validatePasswordReset(user, dto);
        verificationCodeManager.verifyPasswordResetCode(accountSettings, dto.code());
        verificationCodeManager.removePasswordResetCode(accountSettings);

        passwordUpdateService.updatePassword(user, dto.newPassword(), request);
        notifyPasswordChangeService.sendEmail(user);
    }

    private void generateAndSendPasswordResetCode(User user, String email) {
        int code = verificationCodeManager.generatePasswordResetCode(user.getAccountSettings());
        verificationCodeEmailService.sendPasswordResetCode(user, email, code);
    }

    private void validatePasswordReset(User user, PasswordResetConfirmDto dto) {
        credentialValidationService.validateNewPassword(dto.newPassword(), dto.confirmNewPassword(), user.getPassword());
    }

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

}
