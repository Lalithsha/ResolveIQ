package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RetrievalQueryRequest(
    UUID ticketId,
    @NotBlank(message = "Query text is required")
    String queryText,
    String strategy, // HYBRID_RRF, VECTOR_ONLY, FTS_ONLY
    Integer topK
) {}
