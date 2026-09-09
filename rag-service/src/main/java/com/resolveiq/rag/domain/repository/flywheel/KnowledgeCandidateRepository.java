package com.resolveiq.rag.domain.repository.flywheel;

import com.resolveiq.rag.domain.model.flywheel.CandidateEligibilityStatus;
import com.resolveiq.rag.domain.model.flywheel.KnowledgeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeCandidateRepository extends JpaRepository<KnowledgeCandidate, UUID> {

    List<KnowledgeCandidate> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<KnowledgeCandidate> findByTenantIdAndEligibilityStatus(UUID tenantId, CandidateEligibilityStatus status);

    Optional<KnowledgeCandidate> findByIdAndTenantId(UUID id, UUID tenantId);
}
