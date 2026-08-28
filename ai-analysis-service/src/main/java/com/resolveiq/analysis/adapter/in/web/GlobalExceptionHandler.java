package com.resolveiq.analysis.adapter.in.web;

import com.resolveiq.contracts.problem.ProblemDetailResponse;
import com.resolveiq.contracts.tracing.CorrelationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneralException(Exception ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Internal Analysis Error",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage(),
            "ANALYSIS_ERROR",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
}
