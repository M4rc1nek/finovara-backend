package com.finovara.corebackend.limit.exception.notfound;

public class ActiveLimitNotFoundException extends RuntimeException {
    public ActiveLimitNotFoundException(String message) {
        super(message);
    }
}
