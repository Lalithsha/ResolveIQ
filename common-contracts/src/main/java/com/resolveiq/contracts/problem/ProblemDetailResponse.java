package com.resolveiq.contracts.problem;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Standard RFC 7807 Problem Detail response.
 */
public record ProblemDetailResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    String errorCode,
    UUID correlationId,
    Instant timestamp,
    Map<String, Object> invalidParams
) {
    public static ProblemDetailResponse of(
        String title,
        int status,
        String detail,
        String errorCode,
        UUID correlationId
    ) {
        return new ProblemDetailResponse(
            "about:blank",
            title,
            status,
            detail,
            null,
            errorCode,
            correlationId,
            Instant.now(),
            null
        );
    }
}
