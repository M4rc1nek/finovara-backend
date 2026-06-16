package com.finovara.authservice.exception;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.exception.tomanyrequest.VerificationAttemptsExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<AttemptsErrorResponseDto> handleInvalidVerificationCode(InvalidVerificationCodeException exception, WebRequest webRequest) {
        AttemptsErrorResponseDto body = new AttemptsErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", ""),
                exception.getAttempts()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(VerificationAttemptsExceededException.class)
    public ResponseEntity<AttemptsErrorResponseDto> handleToManyVerificationAttempts(VerificationAttemptsExceededException exception, WebRequest webRequest) {
        AttemptsErrorResponseDto body = new AttemptsErrorResponseDto(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", ""),
                exception.getAttempts()
        );
        return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
    }
}
