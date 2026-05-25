package com.finovara.corebackend.exception.unprocessablecontent;

public class MissingRequirementException extends RuntimeException {
    public MissingRequirementException(String message) {
        super(message);
    }
}
