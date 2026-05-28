package com.finovara.contracts.exception.tomanyrequest;

public class TooManyRequests extends RuntimeException {
    public TooManyRequests(String message) {
        super(message);
    }
}
