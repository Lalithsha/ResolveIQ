package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Conversation> findByTicketIdAndTenantId(UUID ticketId, UUID tenantId);
    List<Conversation> findByTenantIdAndPrimaryCustomerIdOrderByUpdatedAtDesc(UUID tenantId, UUID primaryCustomerId);
}
