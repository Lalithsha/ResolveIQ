package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {
    List<Agent> findByTeamIdAndStatusOrderByActiveTicketCountAsc(UUID teamId, String status);
    List<Agent> findByTenantId(UUID tenantId);
}
