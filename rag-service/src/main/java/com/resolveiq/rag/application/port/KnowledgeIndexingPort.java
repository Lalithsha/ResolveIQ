package com.resolveiq.rag.application.port;

import java.util.UUID;

public interface KnowledgeIndexingPort {
    void index(UUID tenantId, UUID documentId, UUID versionId);
}
