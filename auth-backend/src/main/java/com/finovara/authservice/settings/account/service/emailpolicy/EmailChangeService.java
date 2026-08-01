package com.finovara.authservice.settings.account.service.emailpolicy;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.authservice.settings.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeManager;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.email.EmailDomainValidator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
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
    private final VerificationCodeEmailSender verificationCodeEmailSender;
    private final PasswordValidator passwordValidator;
    private final EmailUpdateService emailUpdateService;
    private final EmailDomainValidator emailDomainValidator;
    private final AdditionalAuthorizationService additionalAuthorizationService;

    @Transactional
    public void requestEmailChange(Long userId, EmailChangeRequestDto dto) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, new ConfirmAuthorizationCodeDto(dto.authorizationCode()));
        
        User user = userManagerService.getUserByIdOrThrow(userId);

        validateEmailChangeRequest(user, dto);
        generateAndSendEmailChangeCode(user, dto.email());
    }

    @Transactional
    public AttemptsDto confirmEmailChange(Long userId, EmailChangeConfirmDto dto, HttpServletRequest request) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, new ConfirmAuthorizationCodeDto(dto.authorizationCode()));
        
        User user = userManagerService.getUserByIdOrThrow(userId);
        AccountSettings settings = user.getAccountSettings();

        try {
            verificationCodeManager.verifyEmailChangeCode(settings, dto.code());
        } catch (InvalidInputException exception) {
            AttemptsDto attemptsDto = verificationCodeManager.verifyEmailChangeAttemptsCode(userId, settings);
            throw new InvalidVerificationCodeException(exception.getMessage(), attemptsDto);
        }

        AttemptsDto attemptsDto = verificationCodeManager.getCurrentEmailChangeAttempts(userId);


        String newEmail = settings.getPendingEmail();

        verificationCodeManager.removeEmailChangeCode(settings);

        emailUpdateService.updateEmail(user, newEmail, request);
        return attemptsDto;
    }

    private void validateEmailChangeRequest(User user, EmailChangeRequestDto dto) {
        credentialValidationService.validateEmailChange(user, dto.email());
        emailDomainValidator.validateDomainHasMxRecord(dto.email());
        passwordValidator.validatePassword(user.getId(), new ConfirmPasswordDto(dto.password()));
    }

    private void generateAndSendEmailChangeCode(User user, String newEmail) {
        int code = verificationCodeManager.generateEmailChangeCode(user.getAccountSettings(), newEmail);
        verificationCodeEmailSender.sendEmailChangeCode(user, newEmail, code);
    }
}