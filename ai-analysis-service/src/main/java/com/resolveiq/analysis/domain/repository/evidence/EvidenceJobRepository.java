package com.resolveiq.analysis.domain.repository.evidence;

import com.resolveiq.analysis.domain.model.evidence.EvidenceJob;
import com.resolveiq.analysis.domain.model.evidence.PipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceJobRepository extends JpaRepository<EvidenceJob, UUID> {

    Optional<EvidenceJob> findByIdAndTenantId(UUID id, UUID tenantId);

    List<EvidenceJob> findByTenantIdAndTicketIdOrderByCreatedAtDesc(UUID tenantId, UUID ticketId);

    Optional<EvidenceJob> findByTenantIdAndTicketIdAndAttachmentId(UUID tenantId, UUID ticketId, UUID attachmentId);

    List<EvidenceJob> findByPipelineStatusAndLeaseExpiresAtBefore(PipelineStatus pipelineStatus, Instant now);

    long countByTenantIdAndTicketId(UUID tenantId, UUID ticketId);
}
