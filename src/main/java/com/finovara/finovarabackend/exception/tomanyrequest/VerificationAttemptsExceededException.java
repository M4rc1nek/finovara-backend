package com.finovara.finovarabackend.exception.tomanyrequest;

import com.finovara.finovarabackend.usersetting.account.dto.AttemptsDto;
import lombok.Getter;

@Getter
public class VerificationAttemptsExceededException extends RuntimeException {
    private final int remainingAttempts;
    private final AttemptsDto attempts;

    public VerificationAttemptsExceededException(String message, AttemptsDto attempts) {
        super(message);
        this.attempts = attempts;
        this.remainingAttempts = attempts != null ? attempts.remaining() : 0;
    }

}
