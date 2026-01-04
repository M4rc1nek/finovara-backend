package com.finovara.finovarabackend.exception;

public class MissingRequirementException extends RuntimeException {
    public MissingRequirementException(String message) {
        super(message);
    }
}
