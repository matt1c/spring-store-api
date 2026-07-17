package com.marsmars.util;

import com.marsmars.util.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handlerNotValidException(MethodArgumentNotValidException e) {
        StringBuilder errorMsg = new StringBuilder();
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        for(FieldError error: errors)
            errorMsg.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
        ApiErrorResponse resp = new ApiErrorResponse(
                errorMsg.toString(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handlerException(Exception e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.toString(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<ApiErrorResponse> handlerUserNotFoundException(UserNotFound e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    @ExceptionHandler(UserEmailAlreadyTaken.class)
    public ResponseEntity<ApiErrorResponse> handlerUserEmailAlreadyTakenException(UserEmailAlreadyTaken e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(UserAlreadyBanOrUnbanned.class)
    public ResponseEntity<ApiErrorResponse> handlerUserAlreadyBanOrUnbannedException(UserAlreadyBanOrUnbanned e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(UserRoleAlreadyTaken.class)
    public ResponseEntity<ApiErrorResponse> handlerUserRoleAlreadyTakenException(UserRoleAlreadyTaken e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(ProductNotFound.class)
    public ResponseEntity<ApiErrorResponse> handlerProductNotFoundException(ProductNotFound e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    @ExceptionHandler(OrderNotFound.class)
    public ResponseEntity<ApiErrorResponse> handlerOrderNotFoundException(OrderNotFound e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }
}
