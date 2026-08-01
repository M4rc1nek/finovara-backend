package com.finovara.notificationservice.exception;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import feign.FeignException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({RequestedEntityNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleNotFoundException(RuntimeException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ErrorResponseDto> handleFeignForbidden(FeignException.Forbidden exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(InvalidInputException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailable(ServiceUnavailableException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAll(WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please contact your administrator.",
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
