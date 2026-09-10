package com.koda.ecommerce.user.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static com.koda.ecommerce.user.utils.ErrorCodes.NOT_FOUND;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ReturnObject<Void>> handleApiException(ApiException ex) {
        ReturnObject<Void> body = ReturnObject.fail(ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ReturnObject<Void>> handleNotFound(NoResourceFoundException ex) {
        ReturnObject<Void> body = ReturnObject.fail("Endpoint not found", NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReturnObject<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : "Request validation failed";
        ReturnObject<Void> body = ReturnObject.fail(message, ErrorCodes.VALIDATION_FAILED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReturnObject<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ReturnObject<Void> body = ReturnObject.fail(
                "Unexpected server error", ErrorCodes.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}