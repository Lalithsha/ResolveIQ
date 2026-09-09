package com.resolveiq.rag.domain.repository.flywheel;

import com.resolveiq.rag.domain.model.flywheel.EvaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {

    List<EvaluationRun> findByTenantIdAndCandidateIdOrderByCreatedAtDesc(UUID tenantId, UUID candidateId);

    Optional<EvaluationRun> findByIdAndTenantId(UUID id, UUID tenantId);
}
