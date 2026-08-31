package com.resolveiq.rag.application.dto;

import java.util.List;
import java.util.UUID;

public record ReindexKnowledgeResponse(
    int documentsScanned,
    int versionsReindexed,
    List<UUID> reindexedDocumentIds
) {}
