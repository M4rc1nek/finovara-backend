package com.finovara.corebackend.exception.serviceunavailable;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Exception exception) {
        super(message, exception);
    }
}
