package com.finovara.authservice.util.attempts;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.attempts.dto.AttemptsRegistrationResult;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class VerificationCodeVerifier {

    private final VerificationCodeAttemptsTemplate attemptsTemplate;

    public void verifyOrThrow(Integer storedCode, LocalDateTime expiresAt, Integer providedCode) {
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

    public void verifyAttemptsOrThrow(Integer storedCode, LocalDateTime expiresAt, Integer providedCode, AttemptsContext context, AttemptsHandler handler) {
        boolean attemptsWindowActive = handler.getAttemptsExpiresAt() != null
                && handler.getAttemptsExpiresAt().isAfter(LocalDateTime.now());

        if (attemptsWindowActive && handler.getCurrentAttempts() >= context.maxAttempts()) {
            AttemptsDto attempts = attemptsTemplate.getCurrent(context, handler.getCurrentAttempts());
            throw new VerificationAttemptsExceededException(context.exceededMessage(), attempts);
        }

        try {
            verifyOrThrow(storedCode, expiresAt, providedCode);
        } catch (InvalidInputException ex) {
            AttemptsRegistrationResult registration = attemptsTemplate.registerAttempt(context, handler);

            if (registration.limitExceeded()) {
                throw new VerificationAttemptsExceededException(context.exceededMessage(), registration.attempts());
            }
            throw new InvalidVerificationCodeException(ex.getMessage(), registration.attempts());
        }
    }
}