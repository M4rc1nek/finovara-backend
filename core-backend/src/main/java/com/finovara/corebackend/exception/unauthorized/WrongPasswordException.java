package com.finovara.corebackend.exception.unauthorized;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException(String message) {
        super(message);
    }
}
