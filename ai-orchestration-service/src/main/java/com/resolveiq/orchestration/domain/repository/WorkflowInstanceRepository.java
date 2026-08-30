package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
    Optional<WorkflowInstance> findByTicketId(UUID ticketId);
    Optional<WorkflowInstance> findByTicketIdAndTenantId(UUID ticketId, UUID tenantId);
    Optional<WorkflowInstance> findByIdAndTenantId(UUID id, UUID tenantId);
    List<WorkflowInstance> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<WorkflowInstance> findByTenantIdAndStatus(UUID tenantId, String status);
}
