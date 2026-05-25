package com.finovara.corebackend.limit.exception.conflict;

public class LimitAlreadyExistsException extends RuntimeException {
    public LimitAlreadyExistsException(String message) {
        super(message);
    }
}
