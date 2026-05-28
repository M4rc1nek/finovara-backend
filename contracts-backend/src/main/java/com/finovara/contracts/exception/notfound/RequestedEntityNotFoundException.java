package com.finovara.corebackend.exception.notfound;

public class RequestedEntityNotFoundException extends RuntimeException {
    public RequestedEntityNotFoundException(String message) {
        super(message);
    }
}
