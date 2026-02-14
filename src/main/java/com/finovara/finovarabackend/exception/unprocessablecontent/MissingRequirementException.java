package com.finovara.finovarabackend.exception.unprocessablecontent;

public class MissingRequirementException extends RuntimeException {
    public MissingRequirementException(String message) {
        super(message);
    }
}
