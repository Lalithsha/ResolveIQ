package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Ticket> findByIdAndTenantIdAndCustomerId(UUID id, UUID tenantId, UUID customerId);
    Optional<Ticket> findByTicketNumberAndTenantId(String ticketNumber, UUID tenantId);
    List<Ticket> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId, Pageable pageable);
    List<Ticket> findByTenantIdAndTeamIdOrderByCreatedAtDesc(UUID tenantId, UUID teamId, Pageable pageable);
    List<Ticket> findByTenantIdAndAssignedAgentIdOrderByCreatedAtDesc(UUID tenantId, UUID assignedAgentId, Pageable pageable);
    List<Ticket> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, TicketStatus status, Pageable pageable);
    List<Ticket> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('ticket_schema.ticket_number_seq')", nativeQuery = true)
    Long getNextTicketSequenceVal();
}
