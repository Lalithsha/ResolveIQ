package com.resolveiq.rag.application.service;

import com.resolveiq.rag.domain.model.KnowledgeDocument;
import com.resolveiq.rag.domain.model.KnowledgeVersion;
import com.resolveiq.rag.domain.repository.KnowledgeDocumentRepository;
import com.resolveiq.rag.domain.repository.KnowledgeVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KnowledgePublicationService {
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeVersionRepository versions;

    public KnowledgePublicationService(KnowledgeDocumentRepository documents, KnowledgeVersionRepository versions) {
        this.documents = documents;
        this.versions = versions;
    }

    @Transactional
    public KnowledgeDocument activate(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note) {
        KnowledgeDocument document = requireDocument(tenantId, documentId);
        KnowledgeVersion version = requireVersion(documentId, versionId);
        if (document.getActiveVersionId() != null && !document.getActiveVersionId().equals(versionId)) {
            versions.findByIdAndDocumentId(document.getActiveVersionId(), documentId).ifPresent(current -> {
                current.supersede();
                versions.save(current);
            });
        }
        version.publish(reviewerId, note);
        versions.save(version);
        document.publishVersion(versionId);
        return documents.save(document);
    }

    @Transactional
    public KnowledgeDocument rollback(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note) {
        KnowledgeDocument document = requireDocument(tenantId, documentId);
        KnowledgeVersion target = requireVersion(documentId, versionId);
        if (document.getActiveVersionId() != null && document.getActiveVersionId().equals(versionId)) {
            return document;
        }
        if (document.getActiveVersionId() != null) {
            versions.findByIdAndDocumentId(document.getActiveVersionId(), documentId).ifPresent(current -> {
                current.supersede();
                versions.save(current);
            });
        }
        target.restore(reviewerId, note);
        versions.save(target);
        document.publishVersion(versionId);
        return documents.save(document);
    }

    private KnowledgeDocument requireDocument(UUID tenantId, UUID documentId) {
        return documents.findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
    }

    private KnowledgeVersion requireVersion(UUID documentId, UUID versionId) {
        return versions.findByIdAndDocumentId(versionId, documentId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge version not found: " + versionId));
    }
}
