package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.RoutingDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RoutingDecisionRepository extends JpaRepository<RoutingDecision, UUID> {
    Optional<RoutingDecision> findByTicketId(UUID ticketId);
}
