package com.finovara.authservice.exception;

import com.finovara.authservice.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.authservice.exception.conflict.LocalPasswordNotSetException;
import com.finovara.authservice.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.contracts.exception.ErrorResponseDto;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.forbidden.InvalidPasswordException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
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


    @ExceptionHandler({LocalPasswordNotSetException.class, EntityAlreadyExistsException.class})
    public ResponseEntity<ErrorResponseDto> handleConflict(RuntimeException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleIncorrectCredentials(InvalidCredentialsException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidPassword(InvalidPasswordException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }


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

    @ExceptionHandler({MissingRequirementException.class}) // NEW CODE 422
    public ResponseEntity<ErrorResponseDto> handleUnprocessableEntity(RuntimeException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_CONTENT);
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
