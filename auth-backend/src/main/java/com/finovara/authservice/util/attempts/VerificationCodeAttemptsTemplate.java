package com.finovara.authservice.util.attempts;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.util.attempts.dto.AttemptsContext;
import com.finovara.authservice.util.attempts.dto.AttemptsRegistrationResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class VerificationCodeAttemptsTemplate {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptsRegistrationResult registerAttempt(AttemptsContext context, AttemptsHandler handler) {
        LocalDateTime now = LocalDateTime.now();

        if (handler.getAttemptsExpiresAt() == null || handler.getAttemptsExpiresAt().isBefore(now)) {
            handler.resetAttempts(0, now.plusMinutes(context.attemptsExpirationMinutes()));
        }

        int updated = handler.incrementAttempts(context.maxAttempts());
        int attempts = handler.getCurrentAttempts();
        int remaining = Math.max(0, context.maxAttempts() - attempts);
        AttemptsDto result = new AttemptsDto(attempts, context.maxAttempts(), remaining);

        return new AttemptsRegistrationResult(result, updated == 0);
    }

    public AttemptsDto getCurrent(AttemptsContext context, int currentAttempts) {
        int remaining = Math.max(0, context.maxAttempts() - currentAttempts);
        return new AttemptsDto(currentAttempts, context.maxAttempts(), remaining);
    }
}