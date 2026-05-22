package com.finovara.finovarabackend.exception.tomanyrequest;

public class TooManyRequests extends RuntimeException {
    public TooManyRequests(String message) {
        super(message);
    }
}
