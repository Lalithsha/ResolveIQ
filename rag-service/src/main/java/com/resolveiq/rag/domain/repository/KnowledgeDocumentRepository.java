package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    Optional<KnowledgeDocument> findByIdAndTenantId(UUID id, UUID tenantId);
    List<KnowledgeDocument> findByTenantIdAndStatus(UUID tenantId, String status);
    List<KnowledgeDocument> findByTenantId(UUID tenantId);
}
