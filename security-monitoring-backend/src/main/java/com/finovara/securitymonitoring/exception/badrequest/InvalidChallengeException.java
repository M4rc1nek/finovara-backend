package com.finovara.securitymonitoring.exception.unathorized;

public class InvalidChallengeException extends RuntimeException {
    public InvalidChallengeException(String message) {
        super(message);
    }
}
