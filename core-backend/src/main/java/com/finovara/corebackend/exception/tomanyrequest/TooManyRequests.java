package com.finovara.corebackend.exception.tomanyrequest;

public class TooManyRequests extends RuntimeException {
    public TooManyRequests(String message) {
        super(message);
    }
}
