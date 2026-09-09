package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.HandoffSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface HandoffSummaryRepository extends JpaRepository<HandoffSummary, UUID> {
    List<HandoffSummary> findByTenantIdAndConversationIdOrderByCreatedAtDesc(UUID tenantId, UUID conversationId);
    Optional<HandoffSummary> findFirstByTenantIdAndConversationIdOrderByCreatedAtDesc(UUID tenantId, UUID conversationId);
}
