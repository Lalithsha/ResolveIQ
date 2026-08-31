package com.resolveiq.analysis.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalysisTraceResponse(
    UUID id,
    UUID ticketId,
    String intent,
    String category,
    String modelName,
    String promptVersion,
    String validationOutcome,
    String guardrailOutcome,
    String guardrailFindings,
    int inputTokens,
    int outputTokens,
    long estimatedCostMicros,
    long latencyMs,
    Instant createdAt
) {}
