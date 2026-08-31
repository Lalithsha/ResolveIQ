package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, UUID> {
    List<TicketAttachment> findByTenantIdAndTicketIdOrderByCreatedAtAsc(UUID tenantId, UUID ticketId);
    Optional<TicketAttachment> findByIdAndTenantIdAndTicketId(UUID id, UUID tenantId, UUID ticketId);
    long countByTenantIdAndTicketId(UUID tenantId, UUID ticketId);
}
