package com.finovara.finovarabackend.exception;

public class StateConflictException extends RuntimeException {
    public StateConflictException(String message) {
        super(message);
    }
}
