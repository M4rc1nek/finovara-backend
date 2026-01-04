package com.finovara.finovarabackend.exception;

public record ErrorResponseDTO(
        int status,
        String error,
        String message,
        String path,
        long timestamp
) {

    public ErrorResponseDTO(int status, String error, String message, String path) {
        this(status, error, message, path, System.currentTimeMillis());
    }
}
