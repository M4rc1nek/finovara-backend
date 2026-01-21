package com.finovara.finovarabackend.exception;

public class ActiveLimitNotFoundException extends RuntimeException {
    public ActiveLimitNotFoundException(String message) {
        super(message);
    }
}
