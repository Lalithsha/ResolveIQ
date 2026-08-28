package com.resolveiq.auth.adapter.in.web;

import com.resolveiq.contracts.problem.ProblemDetailResponse;
import com.resolveiq.contracts.tracing.CorrelationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            "INVALID_ARGUMENT",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalState(IllegalStateException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Account Locked",
            HttpStatus.LOCKED.value(),
            ex.getMessage(),
            "ACCOUNT_LOCKED",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.LOCKED).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetailResponse> handleSecurity(SecurityException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Security Violation",
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage(),
            "TOKEN_REUSE_DETECTED",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetailResponse response = new ProblemDetailResponse(
            "about:blank",
            "Validation Failed",
            HttpStatus.BAD_REQUEST.value(),
            "One or more request parameters failed validation",
            null,
            "VALIDATION_ERROR",
            CorrelationContext.getCorrelationId(),
            java.time.Instant.now(),
            errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
