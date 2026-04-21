package com.finovara.finovarabackend.exception.tomanyrequest;

public class TooManyRequestsException extends RuntimeException {
    private final int remainingAttempts;

    public TooManyRequestsException(String message, int remainingAttempts) {
        super(message);
        this.remainingAttempts = remainingAttempts;
    }

}