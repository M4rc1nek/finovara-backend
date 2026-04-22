package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.AttemptsDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeEmailService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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
    private final PasswordUpdateService passwordUpdateService;
    private final UserManagerService userManagerService;

    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(dto.email());

        generateAndSendPasswordResetCode(user, dto.email());
    }

    @Transactional
    public AttemptsDto confirmPasswordReset(PasswordResetConfirmDto dto, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(dto.email());
        AccountSettings settings = user.getAccountSettings();

      AttemptsDto attemptsDto =  verificationCodeManager.verifyPasswordResetAttemptsCode(dto.email(), settings);
        validatePasswordReset(user, dto);
        verificationCodeManager.verifyPasswordResetCode(settings, dto.code());

        verificationCodeManager.removePasswordResetCode(settings);

        passwordUpdateService.updatePassword(user, dto.newPassword(), request);

        return attemptsDto;
    }

    private void generateAndSendPasswordResetCode(User user, String email) {
        int code = verificationCodeManager.generatePasswordResetCode(user.getAccountSettings());
        verificationCodeEmailService.sendPasswordResetCode(user, email, code);
    }

    private void validatePasswordReset(User user, PasswordResetConfirmDto dto) {
        credentialValidationService.validateNewPassword(dto.newPassword(), dto.confirmNewPassword(), user.getPassword());
    }
}
