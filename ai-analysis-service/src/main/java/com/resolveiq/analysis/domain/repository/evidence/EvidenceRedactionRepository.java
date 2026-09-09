package com.resolveiq.analysis.domain.repository.evidence;

import com.resolveiq.analysis.domain.model.evidence.EvidenceRedaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRedactionRepository extends JpaRepository<EvidenceRedaction, UUID> {

    List<EvidenceRedaction> findByTenantIdAndJobId(UUID tenantId, UUID jobId);

    void deleteByTenantIdAndJobId(UUID tenantId, UUID jobId);
}
