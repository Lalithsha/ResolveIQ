package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.RetrievalRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RetrievalRunRepository extends JpaRepository<RetrievalRun, UUID> {
    List<RetrievalRun> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
