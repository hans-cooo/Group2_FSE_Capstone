package com.group2.fse.audit_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global REST exception advisor mapping audit domain exceptions to RFC-7807
 * application/problem+json envelopes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> problem = createProblemDetails(
                "https://api.corebank.local/errors/RESOURCE_NOT_FOUND",
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "RESOURCE_NOT_FOUND"
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> problem = createProblemDetails(
                "https://api.corebank.local/errors/AUTH_ACCESS_DENIED",
                "Forbidden",
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage() != null ? ex.getMessage() : "You do not have permission to access this resource.",
                request.getRequestURI(),
                "AUTH_ACCESS_DENIED"
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Request body validation failed on {}: {} errors", request.getRequestURI(), ex.getBindingResult().getErrorCount());

        List<Map<String, String>> invalidParams = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> errorDetail = new LinkedHashMap<>();
            errorDetail.put("field", fieldError.getField());
            errorDetail.put("message", fieldError.getDefaultMessage());
            invalidParams.add(errorDetail);
        }

        Map<String, Object> problem = createProblemDetails(
                "https://api.corebank.local/errors/VALIDATION_FAILED",
                "Invalid Request Arguments",
                HttpStatus.BAD_REQUEST.value(),
                "One or more input fields failed validation constraints",
                request.getRequestURI(),
                "VALIDATION_FAILED"
        );
        problem.put("invalidParams", invalidParams);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> problem = createProblemDetails(
                "https://api.corebank.local/errors/BAD_REQUEST",
                "Bad Request",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "BAD_REQUEST"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> problem = createProblemDetails(
                "https://api.corebank.local/errors/INTERNAL_SERVER_ERROR",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected server error occurred. Please contact system support.",
                request.getRequestURI(),
                "INTERNAL_SERVER_ERROR"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private Map<String, Object> createProblemDetails(
            String type, String title, int status, String detail, String instance, String errorCode) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", type);
        problem.put("title", title);
        problem.put("status", status);
        problem.put("detail", detail);
        problem.put("instance", instance);
        problem.put("errorCode", errorCode);
        problem.put("timestamp", Instant.now().toString());
        return problem;
    }
}
