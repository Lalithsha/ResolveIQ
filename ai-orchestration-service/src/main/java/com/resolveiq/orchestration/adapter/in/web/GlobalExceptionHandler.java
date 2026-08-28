package com.resolveiq.orchestration.adapter.in.web;

import com.resolveiq.contracts.problem.ProblemDetailResponse;
import com.resolveiq.contracts.tracing.CorrelationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneralException(Exception ex) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
            "Workflow Processing Error",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage(),
            "WORKFLOW_ERROR",
            CorrelationContext.getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
