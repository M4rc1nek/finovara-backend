package com.finovara.financeservice.exception.conflict;

public class ConfirmationRequiredException extends RuntimeException {
    public ConfirmationRequiredException(String message) {
        super(message);
    }
}
