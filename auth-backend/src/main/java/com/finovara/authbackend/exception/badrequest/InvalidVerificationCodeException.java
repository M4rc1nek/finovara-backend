package com.finovara.authbackend.exception.badrequest;

import com.finovara.authbackend.usersetting.account.dto.AttemptsDto;
import lombok.Getter;

@Getter
public class InvalidVerificationCodeException extends RuntimeException {
    private final AttemptsDto attempts;

    public InvalidVerificationCodeException(String message, AttemptsDto attempts) {
        super(message);
        this.attempts = attempts;
    }
}

