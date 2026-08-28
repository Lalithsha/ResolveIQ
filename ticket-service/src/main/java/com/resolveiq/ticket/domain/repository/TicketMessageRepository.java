package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {
    List<TicketMessage> findByTicketIdAndTenantIdOrderByCreatedAtAsc(UUID ticketId, UUID tenantId);
}
