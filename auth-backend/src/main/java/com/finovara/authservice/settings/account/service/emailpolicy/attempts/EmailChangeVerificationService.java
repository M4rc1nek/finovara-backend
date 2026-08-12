package com.finovara.authservice.settings.account.service.emailpolicy.attempts;

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
public class EmailChangeVerificationService {

    private static final String ATTEMPTS_EXCEEDED_MESSAGE = "Email change attempts limit exceeded";

    private final SecretGenerator secretGenerator;
    private final VerificationCodeVerifier verificationCodeVerifier;
    private final VerificationCodeAttemptsTemplate attemptsTemplate;
    private final AccountRepository accountRepository;
    private final VerificationCodeProperties properties;

    public int generateCode(AccountSettings settings, String pendingEmail) {
        int code = secretGenerator.generateSecureCode();
        settings.setEmailChangeCode(code);
        settings.setEmailChangeCodeExpiresAt(LocalDateTime.now().plusMinutes(properties.getCodeExpirationMinutes()));
        settings.setPendingEmail(pendingEmail);
        accountRepository.save(settings);
        return code;
    }

    public void verifyCodeOrThrow(Long userId, AccountSettings settings, Integer providedCode) {
        AttemptsContext context = new AttemptsContext(properties.getMaxAttempts(), properties.getAttemptsExpirationMinutes(), ATTEMPTS_EXCEEDED_MESSAGE);

        verificationCodeVerifier.verifyAttemptsOrThrow(settings.getEmailChangeCode(), settings.getEmailChangeCodeExpiresAt(),
                providedCode, context, new EmailChangeAttemptsHandler(userId, settings, accountRepository));
    }

    public AttemptsDto getCurrentAttempts(Long userId) {
        AttemptsContext context = new AttemptsContext(properties.getMaxAttempts(), properties.getAttemptsExpirationMinutes(), ATTEMPTS_EXCEEDED_MESSAGE);
        return attemptsTemplate.getCurrent(context, accountRepository.getEmailChangeAttemptsByUserId(userId));
    }

    public void removeCode(AccountSettings settings) {
        settings.setEmailChangeCode(null);
        settings.setEmailChangeCodeExpiresAt(null);
        settings.setPendingEmail(null);
        settings.setEmailChangeAttempts(0);
        accountRepository.save(settings);
    }
}