package com.finovara.contracts.exception.conflict;

public class NameAlreadyExistsException extends RuntimeException {
    public NameAlreadyExistsException(String message) {
        super(message);
    }
}
