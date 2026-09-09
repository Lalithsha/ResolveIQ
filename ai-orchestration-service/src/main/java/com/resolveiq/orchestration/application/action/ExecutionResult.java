package com.resolveiq.orchestration.application.action;

import java.util.Map;

public record ExecutionResult(
        String status, // SUCCEEDED, EXECUTION_UNKNOWN, FAILED_RETRYABLE, FAILED_FINAL
        String providerReference,
        Map<String, Object> responsePayload,
        String errorMessage
) {
    public static ExecutionResult success(String providerReference, Map<String, Object> responsePayload) {
        return new ExecutionResult("SUCCEEDED", providerReference, responsePayload, null);
    }

    public static ExecutionResult unknown(String providerReference, String errorMessage) {
        return new ExecutionResult("EXECUTION_UNKNOWN", providerReference, Map.of(), errorMessage);
    }

    public static ExecutionResult failedRetryable(String errorMessage) {
        return new ExecutionResult("FAILED_RETRYABLE", null, Map.of(), errorMessage);
    }

    public static ExecutionResult failedFinal(String errorMessage) {
        return new ExecutionResult("FAILED_FINAL", null, Map.of(), errorMessage);
    }
}
