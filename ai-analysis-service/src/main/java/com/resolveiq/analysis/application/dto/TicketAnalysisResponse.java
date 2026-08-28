package com.resolveiq.analysis.application.dto;

import java.util.UUID;

public record TicketAnalysisResponse(
    UUID analysisId,
    UUID ticketId,
    UUID tenantId,
    String intent,
    String category,
    String sentiment,
    Double sentimentConfidence,
    String urgency,
    Double urgencyConfidence,
    String language,
    String modelName,
    String promptVersion,
    String validationOutcome,
    long latencyMs,
    int tokensUsed
) {}
