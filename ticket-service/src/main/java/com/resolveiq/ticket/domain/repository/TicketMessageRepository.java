package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {
    List<TicketMessage> findByTicketIdAndTenantIdOrderByCreatedAtAsc(UUID ticketId, UUID tenantId);
    List<TicketMessage> findByTicketIdAndTenantIdAndIsInternalFalseOrderByCreatedAtAsc(UUID ticketId, UUID tenantId);
}
