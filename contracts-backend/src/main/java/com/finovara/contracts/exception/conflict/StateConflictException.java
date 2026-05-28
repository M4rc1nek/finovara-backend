package com.finovara.corebackend.exception.conflict;

public class StateConflictException extends RuntimeException {
    public  StateConflictException(String message) {
        super(message);
    }
}
