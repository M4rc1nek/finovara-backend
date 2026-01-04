package com.finovara.finovarabackend.exception;

public class PiggyBankNotFoundException extends RuntimeException {
    public PiggyBankNotFoundException(String message) {
        super(message);
    }
}
