package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
    Optional<AiSuggestion> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AiSuggestion> findByTicketIdAndTenantIdOrderByCreatedAtDesc(UUID ticketId, UUID tenantId);
}
