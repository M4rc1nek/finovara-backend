package com.finovara.corebackend.exception;

import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.corebackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.corebackend.exception.conflict.StateConflictException;
import com.finovara.corebackend.exception.forbidden.NotAuthorizedException;
import com.finovara.corebackend.exception.notfound.WalletNotFoundException;
import com.finovara.corebackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.corebackend.exception.tomanyrequest.TooManyRequests;
import com.finovara.corebackend.exception.tomanyrequest.VerificationAttemptsExceededException;
import com.finovara.corebackend.exception.unauthorized.WrongPasswordException;
import com.finovara.corebackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.corebackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.corebackend.limit.exception.conflict.LimitAlreadyExistsException;
import com.finovara.corebackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.corebackend.limit.exception.unprocessablecontent.LimitExceededException;
import com.finovara.corebackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.corebackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.corebackend.user.exception.notfound.UserNotFoundException;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.corebackend.util.piggybank.exception.notfound.PiggyBankNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EmailAlreadyExistsException.class, NameAlreadyExistsException.class, LimitAlreadyExistsException.class, SmartScanConfirmationRequiredException.class, StateConflictException.class})
    public ResponseEntity<ErrorResponseDto> handleConflictExceptions(RuntimeException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({UserNotFoundException.class, ExpenseNotFoundException.class, RevenueNotFoundException.class,
            WalletNotFoundException.class, PiggyBankNotFoundException.class, ActiveLimitNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleNotFoundException(RuntimeException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<ErrorResponseDto> handleWrongPassword(WrongPasswordException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({MissingRequirementException.class, LimitExceededException.class}) // NOWY KOD 422
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

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleForbidden(NotAuthorizedException exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
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

    @ExceptionHandler(TooManyRequests.class)
    public ResponseEntity<ErrorResponseDto> handleTooManyRequests(TooManyRequests exception, WebRequest webRequest) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                exception.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
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
