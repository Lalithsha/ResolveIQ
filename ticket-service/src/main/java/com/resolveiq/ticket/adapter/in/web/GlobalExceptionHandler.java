package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.contracts.problem.ProblemDetailResponse;
import com.resolveiq.contracts.tracing.CorrelationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Resource Not Found or Invalid",
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            "NOT_FOUND",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalState(IllegalStateException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "State Machine Violation",
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            "ILLEGAL_STATE_TRANSITION",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.resolveiq.ticket.domain.exception.IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetailResponse> handleIdempotencyConflict(com.resolveiq.ticket.domain.exception.IdempotencyConflictException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Idempotency Key Conflict",
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            "IDEMPOTENCY_KEY_REUSED",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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
            "Request failed validation checks",
            null,
            "VALIDATION_ERROR",
            CorrelationContext.getCorrelationId(),
            Instant.now(),
            errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetailResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Malformed Request",
            HttpStatus.BAD_REQUEST.value(),
            "The request body contains invalid JSON or an unsupported value",
            "MALFORMED_REQUEST",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.badRequest().body(response);
    }
}
