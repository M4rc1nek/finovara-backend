package com.finovara.finovarabackend.exception.conflict;

public class NameAlreadyExistsException extends RuntimeException {
    public NameAlreadyExistsException(String message) {
        super(message);
    }
}
