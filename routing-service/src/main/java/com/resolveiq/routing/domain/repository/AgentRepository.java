package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {
    List<Agent> findByTeamIdAndStatusOrderByActiveTicketCountAsc(UUID teamId, String status);
    List<Agent> findByTenantId(UUID tenantId);
}
