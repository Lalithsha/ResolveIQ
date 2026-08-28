package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.SuggestionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SuggestionFeedbackRepository extends JpaRepository<SuggestionFeedback, UUID> {
    List<SuggestionFeedback> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
