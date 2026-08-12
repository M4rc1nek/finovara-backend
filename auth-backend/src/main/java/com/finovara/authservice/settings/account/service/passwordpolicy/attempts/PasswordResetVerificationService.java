package com.finovara.authservice.settings.account.service.passwordpolicy.attempts;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.account.repository.AccountRepository;
import com.finovara.authservice.util.attempts.properties.VerificationCodeProperties;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.attempts.VerificationCodeAttemptsTemplate;
import com.finovara.authservice.util.attempts.VerificationCodeVerifier;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetVerificationService {

    private static final String ATTEMPTS_EXCEEDED_MESSAGE = "Password reset attempts limit exceeded";

    private final SecretGenerator secretGenerator;
    private final VerificationCodeVerifier verificationCodeVerifier;
    private final VerificationCodeAttemptsTemplate attemptsTemplate;
    private final AccountRepository accountRepository;
    private final VerificationCodeProperties properties;

    public int generateCode(AccountSettings settings) {
        int code = secretGenerator.generateSecureCode();
        settings.setResetPasswordCode(code);
        settings.setResetPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(properties.getCodeExpirationMinutes()));
        accountRepository.save(settings);
        return code;
    }

    public void verifyCodeOrThrow(String email, AccountSettings settings, Integer providedCode) {
        AttemptsContext context = new AttemptsContext(properties.getMaxAttempts(), properties.getAttemptsExpirationMinutes(), ATTEMPTS_EXCEEDED_MESSAGE);

        verificationCodeVerifier.verifyAttemptsOrThrow(settings.getResetPasswordCode(), settings.getResetPasswordCodeExpiresAt(),
                providedCode, context, new PasswordResetAttemptsHandler(email, settings, accountRepository));
    }

    public AttemptsDto getCurrentAttempts(String email) {
        AttemptsContext context = new AttemptsContext(properties.getMaxAttempts(), properties.getAttemptsExpirationMinutes(), ATTEMPTS_EXCEEDED_MESSAGE);
        return attemptsTemplate.getCurrent(context, accountRepository.getPasswordResetAttemptsByUserEmail(email));
    }

    public void removeCode(AccountSettings settings) {
        settings.setResetPasswordCode(null);
        settings.setResetPasswordCodeExpiresAt(null);
        settings.setPasswordResetAttempts(0);
        accountRepository.save(settings);
    }
}