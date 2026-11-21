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
    public AttemptsRegistrationResult checkAndRegisterAttempt(AttemptsContext context, AttemptsHandler handler, boolean invalidAttempt) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expiresAt = handler.getAttemptsExpiresAt();
        int currentAttempts = handler.getCurrentAttempts();
        boolean attemptsWindowActive = expiresAt != null && expiresAt.isAfter(now);

        if (attemptsWindowActive && currentAttempts >= context.maxAttempts()) {
            return result(context, currentAttempts, true);
        }

        if (!invalidAttempt) {
            return result(context, currentAttempts, false);
        }

        if (!attemptsWindowActive) {
            handler.resetAttempts(0, now.plusMinutes(context.attemptsExpirationMinutes()));
        }

        int updated = handler.incrementAttempts(context.maxAttempts());
        int attempts = handler.getCurrentAttempts();
        return result(context, attempts, updated == 0);
    }

    public AttemptsDto getCurrent(AttemptsContext context, int currentAttempts) {
        int remaining = Math.max(0, context.maxAttempts() - currentAttempts);
        return new AttemptsDto(currentAttempts, context.maxAttempts(), remaining);
    }

    private AttemptsRegistrationResult result(AttemptsContext context, int currentAttempts, boolean limitExceeded) {
        return new AttemptsRegistrationResult(getCurrent(context, currentAttempts), limitExceeded);
    }
}