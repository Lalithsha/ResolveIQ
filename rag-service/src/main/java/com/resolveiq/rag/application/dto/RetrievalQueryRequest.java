package com.resolveiq.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record RetrievalQueryRequest(
    UUID ticketId,
    @NotBlank(message = "Query text is required")
    @Size(max = 1000, message = "Query text cannot exceed 1000 characters")
    String queryText,
    String strategy, // HYBRID_RRF, VECTOR_ONLY, FTS_ONLY
    Integer topK,
    String category,
    String product,
    String language,
    Set<String> sourceTypes
) {}
