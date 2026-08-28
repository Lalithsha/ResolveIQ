package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, UUID> {
    List<TicketStatusHistory> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
