package com.resolveiq.analysis.domain.repository.evidence;

import com.resolveiq.analysis.domain.model.evidence.EvidenceArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceArtifactRepository extends JpaRepository<EvidenceArtifact, UUID> {

    List<EvidenceArtifact> findByTenantIdAndJobId(UUID tenantId, UUID jobId);

    List<EvidenceArtifact> findByTenantIdAndTicketId(UUID tenantId, UUID ticketId);

    void deleteByTenantIdAndJobId(UUID tenantId, UUID jobId);
}
