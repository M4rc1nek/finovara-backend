package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeManager;
import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeDto;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailRequest;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdditionalAuthorizationEmailService {
    private final VerificationCodeEmailSender verificationCodeEmailSender;
    private final VerificationCodeManager verificationCodeManager;
    private final UserManagerService userManagerService;

    @Transactional
    public void requestAdditionalAuthorizationEmail(Long userId, AdditionalAuthorizationEmailRequest dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        generateAndSendAdditionalAuthorizationCode(user, dto.email());
    }

    @Transactional
    public AttemptsDto confirmAdditionalAuthorizationCode(Long userId, AdditionalAuthorizationEmailCodeDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        SecuritySettings settings = user.getSecuritySettings();

        try {
            verificationCodeManager.verifyAdditionalAuthorizationEmailCode(settings, dto.code());
        } catch (InvalidInputException exception) {
            AttemptsDto attemptsDto = verificationCodeManager.verifyAdditionalAuthorizationConfirmCode(userId, settings);
            throw new InvalidVerificationCodeException(exception.getMessage(), attemptsDto);
        }

        AttemptsDto attemptsDto = verificationCodeManager.getAdditionalAuthorizationEmailCodeAttempts(userId);

        verificationCodeManager.removeAdditionalAuthorizationEmailCode(settings);

        return attemptsDto;
    }

    private void generateAndSendAdditionalAuthorizationCode(User user, String email) {
        int code = verificationCodeManager.generateAdditionalAuthorizationEmailCode(user.getSecuritySettings());
        verificationCodeEmailSender.sendAuthorizationConfirmCode(user, email, code);
    }
}
