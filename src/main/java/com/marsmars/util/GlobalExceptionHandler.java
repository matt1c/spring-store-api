package com.marsmars.util;

import com.marsmars.util.exceptions.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.rmi.AccessException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handlerNotValidException(MethodArgumentNotValidException e) {
        StringBuilder errorMsg = new StringBuilder();
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        for (FieldError error : errors) {
            errorMsg.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
        }
        ApiErrorResponse resp = new ApiErrorResponse(
                errorMsg.toString(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler({
            SQLException.class, OutOfMemoryError.class, NullPointerException.class,
            ArithmeticException.class, IndexOutOfBoundsException.class,
            NumberFormatException.class, DataAccessException.class, ClassNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handlerInternalServerErrors(Throwable e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            BindException.class, IllegalArgumentException.class, IllegalStateException.class
    })
    public ResponseEntity<ApiErrorResponse> handlerBadRequestExceptions(Exception e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler({
            UserNotFound.class, ProductNotFound.class,
            OrderNotFound.class, RoleNotFound.class
    })
    public ResponseEntity<ApiErrorResponse> handlerNotFoundExceptions(RuntimeException e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    @ExceptionHandler({UserEmailAlreadyTaken.class, UserRoleAlreadyTaken.class,
            InsufficientStockException.class, UserPasswordIsAlreadyValid.class, UserAlreadyBanOrUnbanned.class})
    public ResponseEntity<ApiErrorResponse> handlerConflictExceptions(RuntimeException e) {
        System.out.println("EXCEPTION MESSAGE: " + e.getMessage());
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
    }

    @ExceptionHandler({AccessDeniedException.class, AccessException.class})
    public ResponseEntity<ApiErrorResponse> handlerAccessDeniedExceptions(Exception e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handlerAllExceptions(Exception e) {
        ApiErrorResponse resp = new ApiErrorResponse(
                "Произошла непредвиденная ошибка: " + e.getMessage(),
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}