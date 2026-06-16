package com.finovara.authservice.settings.account.service.passwordpolicy;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.authservice.settings.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.service.passwordpolicy.change.PasswordUpdateService;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeManager;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailService;
import com.finovara.authservice.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

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

        validatePasswordReset(user, dto);
        try {
            verificationCodeManager.verifyPasswordResetCode(settings, dto.code());
        } catch (InvalidInputException exception) {
            AttemptsDto attemptsDto = verificationCodeManager.verifyPasswordResetAttemptsCode(dto.email(), settings);
            throw new InvalidVerificationCodeException(exception.getMessage(), attemptsDto);
        }

        AttemptsDto attemptsDto = verificationCodeManager.getCurrentPasswordResetAttempts(dto.email());

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
