package com.resolveiq.rag.application.dto;

import java.util.List;
import java.util.UUID;

public record RetrievalResultDto(
    UUID retrievalRunId,
    String queryText,
    String strategy,
    long durationMs,
    List<CitationDto> citations
) {}
