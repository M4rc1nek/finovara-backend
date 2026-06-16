package com.finovara.authservice.exception.badrequest;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import lombok.Getter;

@Getter
public class InvalidVerificationCodeException extends RuntimeException {
    private final AttemptsDto attempts;

    public InvalidVerificationCodeException(String message, AttemptsDto attempts) {
        super(message);
        this.attempts = attempts;
    }
}

