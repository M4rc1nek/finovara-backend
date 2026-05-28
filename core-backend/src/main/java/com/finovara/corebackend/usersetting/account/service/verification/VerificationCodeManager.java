package com.finovara.corebackend.usersetting.account.service.verification;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.corebackend.usersetting.account.dto.AttemptsDto;
import com.finovara.corebackend.usersetting.account.model.AccountSettings;
import com.finovara.corebackend.usersetting.account.repository.AccountRepository;
import com.finovara.corebackend.usersetting.account.service.verification.properties.VerificationCodeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationCodeManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final VerificationCodeProperties verificationCodeProperties;

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptsDto verifyEmailChangeAttemptsCode(Long userId, AccountSettings settings) {
        LocalDateTime now = LocalDateTime.now();

        if (settings.getAttemptsEmailExpiresAt() == null || settings.getAttemptsEmailExpiresAt().isBefore(now)) {
            settings.setEmailChangeAttempts(0);
            settings.setAttemptsEmailExpiresAt(now.plusMinutes(verificationCodeProperties.getAttemptsExpirationMinutes()));
            accountRepository.save(settings);
        }

        int updated = accountRepository.incrementEmailChangeAttempts(userId, verificationCodeProperties.getMaxAttempts());
        int attempts = accountRepository.getEmailChangeAttemptsByUserId(userId);
        int remainingAttempts = Math.max(0, verificationCodeProperties.getMaxAttempts() - attempts);
        AttemptsDto attemptsDto = new AttemptsDto(attempts, verificationCodeProperties.getMaxAttempts(), remainingAttempts);

        if (updated == 0) {
            throw new VerificationAttemptsExceededException("Email change attempts limit exceeded", attemptsDto);
        }

        return attemptsDto;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptsDto verifyPasswordResetAttemptsCode(String email, AccountSettings settings) {
        LocalDateTime now = LocalDateTime.now();

        if (settings.getAttemptsPasswordExpiresAt() == null || settings.getAttemptsPasswordExpiresAt().isBefore(now)) {
            settings.setPasswordResetAttempts(0);
            settings.setAttemptsPasswordExpiresAt(now.plusMinutes(verificationCodeProperties.getAttemptsExpirationMinutes()));
            accountRepository.save(settings);
        }

        int updated = accountRepository.incrementPasswordResetAttempts(email, verificationCodeProperties.getMaxAttempts());
        int attempts = accountRepository.getPasswordResetAttemptsByUserEmail(email);
        int remainingAttempts = Math.max(0, verificationCodeProperties.getMaxAttempts() - attempts);
        AttemptsDto attemptsDto = new AttemptsDto(attempts, verificationCodeProperties.getMaxAttempts(), remainingAttempts);

        if (updated == 0) {
            throw new VerificationAttemptsExceededException("Password reset attempts limit exceeded", attemptsDto);
        }

        return attemptsDto;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptsDto getCurrentEmailChangeAttempts(Long userId) {
        int attempts = accountRepository.getEmailChangeAttemptsByUserId(userId);
        int remainingAttempts = Math.max(0, verificationCodeProperties.getMaxAttempts() - attempts);
        return new AttemptsDto(attempts, verificationCodeProperties.getMaxAttempts(), remainingAttempts);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptsDto getCurrentPasswordResetAttempts(String email) {
        int attempts = accountRepository.getPasswordResetAttemptsByUserEmail(email);
        int remainingAttempts = Math.max(0, verificationCodeProperties.getMaxAttempts() - attempts);
        return new AttemptsDto(attempts, verificationCodeProperties.getMaxAttempts(), remainingAttempts);

    }

    public int generatePasswordResetCode(AccountSettings settings) {
        int code = generateCode();
        settings.setResetPasswordCode(code);
        settings.setResetPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(verificationCodeProperties.getCodeExpirationMinutes()));
        accountRepository.save(settings);
        return code;
    }

    public int generateEmailChangeCode(AccountSettings settings, String pendingEmail) {
        int code = generateCode();
        settings.setEmailChangeCode(code);
        settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().plusMinutes(verificationCodeProperties.getCodeExpirationMinutes()));
        settings.setPendingEmail(pendingEmail);
        accountRepository.save(settings);
        return code;
    }

    public void verifyPasswordResetCode(AccountSettings settings, Integer providedCode) {
        verifyCode(settings.getResetPasswordCode(), settings.getResetPasswordCodeExpiresAt(), providedCode);
    }

    public void verifyEmailChangeCode(AccountSettings settings, Integer providedCode) {
        verifyCode(settings.getEmailChangeCode(), settings.getEmailChangeCodeExpiresAt(), providedCode);
    }

    public void removePasswordResetCode(AccountSettings settings) {
        settings.setResetPasswordCode(null);
        settings.setResetPasswordCodeExpiresAt(null);
        settings.setPasswordResetAttempts(0);
        accountRepository.save(settings);
    }

    public void removeEmailChangeCode(AccountSettings settings) {
        settings.setEmailChangeCode(null);
        settings.setEmailChangeCodeExpiresAt(null);
        settings.setPendingEmail(null);
        settings.setEmailChangeAttempts(0);
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

