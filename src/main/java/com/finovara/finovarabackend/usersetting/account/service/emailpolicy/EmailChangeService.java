package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.security.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.security.VerificationCodeManager;
import com.finovara.finovarabackend.usersetting.account.service.security.VerificationCodeEmailService;
import com.finovara.finovarabackend.usersetting.notificationemail.action.emailchange.service.NotifyEmailChangeService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailChangeService {

    private final UserManagerService userManagerService;
    private final CredentialValidationService credentialValidationService;
    private final VerificationCodeManager verificationCodeManager;
    private final VerificationCodeEmailService verificationCodeEmailService;
    private final PasswordValidator passwordValidator;
    private final EmailUpdateService emailUpdateService;

    @Transactional
    public void requestEmailChange(Long userId, EmailChangeRequestDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        validateEmailChangeRequest(user, dto);
        generateAndSendEmailChangeCode(user, dto.email());
    }

    @Transactional
    public void confirmEmailChange(Long userId, EmailChangeConfirmDto dto, HttpServletRequest request) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings settings = user.getAccountSettings();

        verificationCodeManager.verifyEmailChangeCode(settings, dto.code());

        String newEmail = settings.getPendingEmail();

        verificationCodeManager.removeEmailChangeCode(settings);

        emailUpdateService.updateEmail(user, newEmail, request);
    }

    private void validateEmailChangeRequest(User user, EmailChangeRequestDto dto) {
        credentialValidationService.validateEmailChange(user, dto.email());
        passwordValidator.validatePassword(user.getId(), new ConfirmPasswordDto(dto.password()));
    }

    private void generateAndSendEmailChangeCode(User user, String newEmail) {
        int code = verificationCodeManager.generateEmailChangeCode(user.getAccountSettings(), newEmail);
        verificationCodeEmailService.sendEmailChangeCode(user, newEmail, code);
    }
}