package com.resolveiq.rag.application.port;

import com.resolveiq.rag.domain.model.KnowledgeDocument;
import java.util.UUID;

public interface KnowledgePublicationPort {
    KnowledgeDocument activate(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note);
    KnowledgeDocument rollback(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note);
}
