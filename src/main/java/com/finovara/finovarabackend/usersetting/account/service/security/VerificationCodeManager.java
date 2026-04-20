package com.finovara.finovarabackend.usersetting.account.service.security;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationCodeManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_EXPIRATION_MINUTES = 15;

    private final AccountRepository accountRepository;

    public int generatePasswordResetCode(AccountSettings settings) {
        int code = generateCode();
        settings.setForgotPasswordCode(code);
        settings.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES));
        accountRepository.save(settings);
        return code;
    }

    public int generateEmailChangeCode(AccountSettings settings, String pendingEmail) {
        int code = generateCode();
        settings.setEmailChangeCode(code);
        settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES));
        settings.setPendingEmail(pendingEmail);
        accountRepository.save(settings);
        return code;
    }

    public void verifyPasswordResetCode(AccountSettings settings, Integer providedCode) {
        verifyCode(settings.getForgotPasswordCode(), settings.getForgotPasswordCodeExpiresAt(), providedCode);
    }

    public void verifyEmailChangeCode(AccountSettings settings, Integer providedCode) {
        verifyCode(settings.getEmailChangeCode(), settings.getEmailChangeCodeExpiresAt(), providedCode);
    }

    public void removePasswordResetCode(AccountSettings settings) {
        settings.setForgotPasswordCode(null);
        settings.setForgotPasswordCodeExpiresAt(null);
        accountRepository.save(settings);
    }

    public void removeEmailChangeCode(AccountSettings settings) {
        settings.setEmailChangeCode(null);
        settings.setEmailChangeCodeExpiresAt(null);
        settings.setPendingEmail(null);
        accountRepository.save(settings);
    }

    private int generateCode() {
        return SECURE_RANDOM.nextInt(900000) + 100000;
    }

    private void verifyCode(Integer storedCode, LocalDateTime expiresAt, Integer providedCode) {
        if (providedCode == null) {
            throw new InvalidInputException("Code is required");
        }

        if (storedCode == null) {
            throw new InvalidInputException("No code generated");
        }

        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new InvalidInputException("Code expired");
        }

        if (!storedCode.equals(providedCode)) {
            throw new InvalidInputException("Incorrect code");
        }
    }
}

