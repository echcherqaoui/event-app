package com.eventapp.sharedutils.exceptions;


import com.eventapp.sharedutils.exceptions.enums.CommonErrorCode;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.INTERNAL_SERVER_ERROR;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> buildValidationErrorResponse(Set<String> errors, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
              CommonErrorCode.VALIDATION_FAILED,
              "Input validation failed",
              request.getDescription(false)
        );
        response.setValidationErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BaseCustomException.class)
    public ResponseEntity<ErrorResponse> customExceptionHandler(BaseCustomException ex, WebRequest request) {
        IErrorCode errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : INTERNAL_SERVER_ERROR;

        ErrorResponse response = new ErrorResponse(
              errorCode,
              ex.getMessage(),
              request.getDescription(false)
        );

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    // Handles DTO validation (JSON bodies)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> dtoValidationHandler(MethodArgumentNotValidException ex, WebRequest request) {
        Set<String> errors = ex.getBindingResult()
              .getFieldErrors()
              .stream()
              .map(error -> error.getField() + ": " + error.getDefaultMessage())
              .collect(Collectors.toSet());

        return buildValidationErrorResponse(errors, request);
    }

    // Handles Simple Param validation (PathVariables/QueryParams)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> paramValidationHandler(ConstraintViolationException ex, WebRequest request) {
        Set<String> errors = ex.getConstraintViolations()
              .stream()
              .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
              .collect(Collectors.toSet());

        return buildValidationErrorResponse(errors, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception [ID: {}]: {}", errorId, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
              INTERNAL_SERVER_ERROR,
              INTERNAL_SERVER_ERROR.formatMessage(errorId),
              request.getDescription(false)
        );

        return ResponseEntity
              .status(INTERNAL_SERVER_ERROR.getHttpStatus())
              .body(response);
    }
}