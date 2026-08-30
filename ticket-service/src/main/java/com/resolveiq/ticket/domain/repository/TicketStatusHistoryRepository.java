package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, UUID> {
    List<TicketStatusHistory> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
