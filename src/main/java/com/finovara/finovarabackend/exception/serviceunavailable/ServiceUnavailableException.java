package com.finovara.finovarabackend.exception.serviceunavailable;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Exception exception) {
        super(message, exception);
    }
}
