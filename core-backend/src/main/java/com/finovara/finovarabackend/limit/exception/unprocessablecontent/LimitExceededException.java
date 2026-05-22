package com.finovara.finovarabackend.limit.exception.unprocessablecontent;

public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}
