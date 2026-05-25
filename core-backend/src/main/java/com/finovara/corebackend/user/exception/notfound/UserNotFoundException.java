package com.finovara.corebackend.user.exception.notfound;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
