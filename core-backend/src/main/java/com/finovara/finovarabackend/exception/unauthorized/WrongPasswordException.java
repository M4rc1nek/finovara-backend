package com.finovara.finovarabackend.exception.unauthorized;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException(String message) {
        super(message);
    }
}
