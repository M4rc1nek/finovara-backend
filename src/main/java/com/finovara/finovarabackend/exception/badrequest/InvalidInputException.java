package com.finovara.finovarabackend.exception.badrequest;

public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}
