package com.finovara.finovarabackend.exception;

public class LimitAlreadyExistsException extends RuntimeException {
    public LimitAlreadyExistsException(String message) {
        super(message);
    }
}
