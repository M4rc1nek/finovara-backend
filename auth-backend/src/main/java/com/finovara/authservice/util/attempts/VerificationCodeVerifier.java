package com.finovara.authservice.util.attempts;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.exception.tomanyrequest.VerificationAttemptsExceededException;
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

    private void verifyOrThrow(Integer storedCode, LocalDateTime expiresAt, Integer providedCode) {
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
        try {
            verifyOrThrow(storedCode, expiresAt, providedCode);
        } catch (InvalidInputException exception) {
            AttemptsRegistrationResult registration = attemptsTemplate.checkAndRegisterAttempt(context, handler, true);
            if (registration.limitExceeded()) {
                throw new VerificationAttemptsExceededException(context.exceededMessage(), registration.attempts());
            }
            throw new InvalidVerificationCodeException(exception.getMessage(), registration.attempts());
        }

        AttemptsRegistrationResult registration = attemptsTemplate.checkAndRegisterAttempt(context, handler, false);
        if (registration.limitExceeded()) {
            throw new VerificationAttemptsExceededException(context.exceededMessage(), registration.attempts());
        }
    }
}