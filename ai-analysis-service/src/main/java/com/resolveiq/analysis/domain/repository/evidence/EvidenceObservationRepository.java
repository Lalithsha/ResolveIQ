package com.resolveiq.analysis.domain.repository.evidence;

import com.resolveiq.analysis.domain.model.evidence.EvidenceObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceObservationRepository extends JpaRepository<EvidenceObservation, UUID> {

    List<EvidenceObservation> findByTenantIdAndJobId(UUID tenantId, UUID jobId);

    List<EvidenceObservation> findByTenantIdAndTicketId(UUID tenantId, UUID ticketId);

    void deleteByTenantIdAndJobId(UUID tenantId, UUID jobId);
}
