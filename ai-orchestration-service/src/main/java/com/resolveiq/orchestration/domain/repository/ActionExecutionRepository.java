package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.action.ActionExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

@Repository
public interface ActionExecutionRepository extends JpaRepository<ActionExecution, UUID> {
    List<ActionExecution> findByProposalIdAndTenantIdOrderByAttemptNumberAsc(UUID proposalId, UUID tenantId);
    Optional<ActionExecution> findByTenantIdAndProviderIdempotencyKey(UUID tenantId, String providerIdempotencyKey);

    Optional<ActionExecution> findByTenantIdAndClientIdempotencyKey(UUID tenantId, String clientIdempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ActionExecution e where (e.status = 'QUEUED' or (e.status = 'FAILED_RETRYABLE' and e.attemptNumber < 3) or (e.status = 'CLAIMED' and e.leaseExpiresAt < :now)) order by e.startedAt")
    List<ActionExecution> lockRecoverable(@Param("now") Instant now, org.springframework.data.domain.Pageable pageable);
}
