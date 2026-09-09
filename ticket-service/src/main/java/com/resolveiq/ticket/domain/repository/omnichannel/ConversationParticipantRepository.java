package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    List<ConversationParticipant> findByTenantIdAndConversationId(UUID tenantId, UUID conversationId);
}
