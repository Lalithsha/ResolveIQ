package com.resolveiq.rag.application.dto;

import java.util.UUID;

public record CitationDto(
    String sourceType, // KNOWLEDGE_ARTICLE, RESOLVED_CASE
    UUID sourceId,
    UUID versionId,
    UUID chunkId,
    String title,
    String citationText,
    Double score
) {}
