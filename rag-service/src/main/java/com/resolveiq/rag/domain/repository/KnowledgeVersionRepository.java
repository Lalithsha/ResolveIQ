package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.KnowledgeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeVersionRepository extends JpaRepository<KnowledgeVersion, UUID> {
    List<KnowledgeVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);
    Optional<KnowledgeVersion> findTopByDocumentIdOrderByVersionNumberDesc(UUID documentId);
}
