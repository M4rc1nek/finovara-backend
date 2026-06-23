package com.finovara.financeservice.exception;

import com.finovara.financeservice.exception.conflict.QuantityLimitOperationException;
import com.finovara.financeservice.exception.conflict.SmartScanConfirmationRequiredException;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({QuantityLimitOperationException.class, SmartScanConfirmationRequiredException.class})
    public ResponseEntity<ErrorResponseDto> handleConflictExceptions(RuntimeException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);

    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ErrorResponseDto> handleFeignForbidden(FeignException.Forbidden exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Incorrect password",
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }
}
