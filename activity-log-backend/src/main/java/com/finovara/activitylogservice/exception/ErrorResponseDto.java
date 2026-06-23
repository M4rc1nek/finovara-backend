package com.finovara.activitylogservice.exception;

public record ErrorResponseDto(
        int status,
        String error,
        String message,
        String path,
        long timestamp
) {

    public ErrorResponseDto(int status, String error, String message, String path) {
        this(status, error, message, path, System.currentTimeMillis());
    }
}
