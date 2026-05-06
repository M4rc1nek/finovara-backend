package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.AttemptsDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeEmailService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.finovarabackend.util.email.EmailDomainValidator;
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
    private final EmailDomainValidator emailDomainValidator;

    @Transactional
    public void requestEmailChange(Long userId, EmailChangeRequestDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        validateEmailChangeRequest(user, dto);
        generateAndSendEmailChangeCode(user, dto.email());
    }

    @Transactional
    public AttemptsDto confirmEmailChange(Long userId, EmailChangeConfirmDto dto, HttpServletRequest request) {
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
        verificationCodeEmailService.sendEmailChangeCode(user, newEmail, code);
    }
}