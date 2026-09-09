package com.resolveiq.rag.domain.repository.flywheel;

import com.resolveiq.rag.domain.model.flywheel.KnowledgeRelease;
import com.resolveiq.rag.domain.model.flywheel.KnowledgeReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeReleaseRepository extends JpaRepository<KnowledgeRelease, UUID> {

    List<KnowledgeRelease> findByTenantIdOrderByReleasedAtDesc(UUID tenantId);

    Optional<KnowledgeRelease> findByTenantIdAndStatus(UUID tenantId, KnowledgeReleaseStatus status);

    Optional<KnowledgeRelease> findByIdAndTenantId(UUID id, UUID tenantId);
}
