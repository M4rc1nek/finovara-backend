package com.finovara.contracts.exception.notfound;

public class RequestedEntityNotFoundException extends RuntimeException {
    public RequestedEntityNotFoundException(String message) {
        super(message);
    }
}
