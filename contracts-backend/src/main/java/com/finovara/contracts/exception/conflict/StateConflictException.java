package com.finovara.contracts.exception.conflict;

public class StateConflictException extends RuntimeException {
    public  StateConflictException(String message) {
        super(message);
    }
}
