package com.finovara.finovarabackend.limit.exception.conflict;

public class LimitAlreadyExistsException extends RuntimeException {
    public LimitAlreadyExistsException(String message) {
        super(message);
    }
}
